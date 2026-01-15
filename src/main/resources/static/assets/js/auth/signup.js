(() => {
    const byId = (id) => document.getElementById(id);

    const form = byId("signupForm");

    // ---------------- Global alert ----------------
    const globalAlert = byId("globalErrorAlert");
    function showGlobalError(msg) {
        if (!globalAlert) return;
        globalAlert.textContent = msg || "현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.";
        globalAlert.classList.remove("d-none");
    }
    function clearGlobalError() {
        if (!globalAlert) return;
        globalAlert.textContent = "";
        globalAlert.classList.add("d-none");
    }

    // ---------------- Field configs ----------------
    const configs = [
        { name: "username", input: byId("username"), feedback: byId("usernameFeedback"), availability: true },
        { name: "password", input: byId("password"), feedback: byId("passwordFeedback"), availability: false },
        { name: "email", input: byId("email"), feedback: byId("emailFeedback"), availability: true },
        { name: "nickname", input: byId("nickname"), feedback: byId("nicknameFeedback"), availability: true }
    ];

    // availability 상태(중복 검사 결과) 보관
    const availabilityState = {
        username: { ok: false, last: null, reason: "", controller: null },
        email: { ok: false, last: null, reason: "", controller: null },
        nickname: { ok: false, last: null, reason: "", controller: null }
    };

    // ---------------- OTP refs ----------------
    const emailVerifyBtn = byId("emailVerifyBtn");
    const otpInput = byId("otp");
    const otpVerifyBtn = byId("otpVerifyBtn");
    const otpResendBtn = byId("otpResendBtn");
    const otpFeedback = byId("otpFeedback");
    const otpTimerEl = byId("otpTimer");
    const resendTimerEl = byId("resendTimer");

    const OTP_SEND_URL = form?.dataset?.otpSendUrl || "/signup/email-verifications";
    const OTP_VERIFY_URL = form?.dataset?.otpVerifyUrl || "/signup/email-verifications/verify";
    const SIGNUP_URL = form?.getAttribute("action") || "/signup";

    const emailProofState = {
        sending: false,
        verifying: false,
        verified: false,
        otpRemaining: 0,
        resendRemaining: 0,
        otpInterval: null,
        resendInterval: null
    };

    // ---------------- ApiResponse.code 매핑(당신 enum 기준) ----------------
    const CODES = {
        USERNAME_DUP: "AUTH_CREDENTIAL_E_002",
        EMAIL_DUP: "AUTH_CREDENTIAL_E_003",
        NICK_DUP: "AUTH_CREDENTIAL_E_004",

        EMAIL_MISMATCH: "AUTH_EMAIL_VERIFICATION_E_003",
        PROOF_EXPIRED: "AUTH_EMAIL_VERIFICATION_E_005" // 일단 여기까지만
    };

    // ---------------- common ui helpers ----------------
    function setFeedback(cfg, type, text) {
        const fb = cfg.feedback;
        if (!fb) return;

        fb.textContent = text || "";
        fb.classList.remove("ok", "bad", "info");
        if (!text) return;
        fb.classList.add(type);
    }

    function setFeedbackHtml(cfg, type, html) {
        const fb = cfg.feedback;
        if (!fb) return;

        fb.innerHTML = html || "";
        fb.classList.remove("ok", "bad", "info");
        if (!html) return;
        fb.classList.add(type);
    }

    function setInputState(cfg, state /* 'is-valid' | 'is-invalid' | null */) {
        const el = cfg.input;
        if (!el) return;
        el.classList.remove("is-valid", "is-invalid");
        if (state) el.classList.add(state);
    }

    function readAttr(cfg, key, fallback = null) {
        return cfg.input?.dataset?.[key] ?? fallback;
    }

    function valueOf(cfg) {
        const raw = cfg.input?.value ?? "";
        if (cfg.name === "password") return raw; // password는 trim 금지
        return raw.trim();
    }

    function applyFieldError(fieldName, msg) {
        const cfg = configs.find((c) => c.name === fieldName);
        if (!cfg) return;
        setInputState(cfg, "is-invalid");
        setFeedback(cfg, "bad", msg || "값을 확인해주세요.");
        cfg.input?.focus();
    }

    // ---------------- OTP helpers ----------------
    function setOtpFeedback(type, text) {
        if (!otpFeedback) return;
        otpFeedback.textContent = text || "";
        otpFeedback.classList.remove("ok", "bad", "info");
        if (!text) return;
        otpFeedback.classList.add(type);
    }

    function fmtMMSS(totalSeconds) {
        const s = Math.max(0, Number(totalSeconds) || 0);
        const mm = String(Math.floor(s / 60)).padStart(2, "0");
        const ss = String(s % 60).padStart(2, "0");
        return `${mm}:${ss}`;
    }

    function renderTimers() {
        if (otpTimerEl) otpTimerEl.textContent = fmtMMSS(emailProofState.otpRemaining || 0);
        if (resendTimerEl) resendTimerEl.textContent = fmtMMSS(emailProofState.resendRemaining || 0);
    }

    function clearOtpIntervals() {
        if (emailProofState.otpInterval) clearInterval(emailProofState.otpInterval);
        if (emailProofState.resendInterval) clearInterval(emailProofState.resendInterval);
        emailProofState.otpInterval = null;
        emailProofState.resendInterval = null;
    }

    function resetEmailProofUi() {
        clearOtpIntervals();

        emailProofState.sending = false;
        emailProofState.verifying = false;
        emailProofState.verified = false;
        emailProofState.otpRemaining = 0;
        emailProofState.resendRemaining = 0;

        renderTimers();

        if (otpInput) {
            otpInput.value = "";
            otpInput.disabled = true;
            otpInput.classList.remove("is-valid", "is-invalid");
        }
        if (otpVerifyBtn) otpVerifyBtn.disabled = true;
        if (otpResendBtn) otpResendBtn.disabled = true;

        setOtpFeedback("", "");
        if (emailVerifyBtn) {
            emailVerifyBtn.textContent = "이메일 인증";
            emailVerifyBtn.disabled = true;
        }
    }

    function updateEmailVerifyBtnState() {
        if (!emailVerifyBtn) return;

        if (emailProofState.verified || emailProofState.sending) {
            emailVerifyBtn.disabled = true;
            return;
        }

        const emailCfg = configs.find((c) => c.name === "email");
        const emailEl = emailCfg?.input;
        if (!emailEl) {
            emailVerifyBtn.disabled = true;
            return;
        }

        const emailValid = emailEl.classList.contains("is-valid");
        const emailAvailable = Boolean(availabilityState.email.ok);

        emailVerifyBtn.disabled = !(emailValid && emailAvailable);
    }

    function startOtpCountdown(seconds) {
        emailProofState.otpRemaining = Math.max(0, Number(seconds) || 0);
        renderTimers();

        if (emailProofState.otpInterval) clearInterval(emailProofState.otpInterval);
        emailProofState.otpInterval = setInterval(() => {
            emailProofState.otpRemaining = Math.max(0, emailProofState.otpRemaining - 1);
            renderTimers();

            if (emailProofState.otpRemaining <= 0) {
                clearInterval(emailProofState.otpInterval);
                emailProofState.otpInterval = null;

                if (!emailProofState.verified) {
                    if (otpVerifyBtn) otpVerifyBtn.disabled = true;
                    if (otpInput) otpInput.disabled = true;
                    setOtpFeedback("bad", "OTP가 만료되었습니다. 재전송 후 다시 인증해주세요.");
                }
            }
        }, 1000);
    }

    function startResendCooldown(seconds) {
        emailProofState.resendRemaining = Math.max(0, Number(seconds) || 0);
        renderTimers();

        if (otpResendBtn) otpResendBtn.disabled = emailProofState.resendRemaining > 0;

        if (emailProofState.resendInterval) clearInterval(emailProofState.resendInterval);
        emailProofState.resendInterval = setInterval(() => {
            emailProofState.resendRemaining = Math.max(0, emailProofState.resendRemaining - 1);
            renderTimers();

            if (emailProofState.resendRemaining <= 0) {
                clearInterval(emailProofState.resendInterval);
                emailProofState.resendInterval = null;

                if (!emailProofState.verified && otpResendBtn) {
                    otpResendBtn.disabled = false;
                }
            }
        }, 1000);
    }

    async function postJson(url, body) {
        const res = await fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: { Accept: "application/json", "Content-Type": "application/json" },
            body: JSON.stringify(body ?? {})
        });

        let json = null;
        try {
            json = await res.json();
        } catch (_) {}
        return { res, json };
    }

    function validateOtpLocal(otpStr) {
        const v = (otpStr ?? "").trim();
        if (!/^\d{6}$/.test(v)) return "OTP는 6자리 숫자입니다.";
        const n = Number(v);
        if (!Number.isFinite(n) || n < 1 || n > 999999) return "OTP는 000001~999999 범위입니다.";
        return null;
    }

    // ---------------- OTP send/verify ----------------
    async function sendEmailVerification() {
        clearGlobalError();

        if (emailProofState.sending || emailProofState.verified) return;

        const emailValue = (configs.find((c) => c.name === "email")?.input?.value ?? "").trim();
        if (!emailValue) {
            setOtpFeedback("bad", "이메일을 입력해주세요.");
            return;
        }

        emailProofState.sending = true;
        if (emailVerifyBtn) emailVerifyBtn.disabled = true;

        setOtpFeedback("info", "인증번호 전송 중...");

        try {
            const { res, json } = await postJson(OTP_SEND_URL, { email: emailValue });

            if (!res.ok && (res.status === 503 || res.status >= 500)) {
                showGlobalError(json?.message);
                setOtpFeedback("", "");
                return;
            }

            if (!res.ok || !json || json.success === false) {
                setOtpFeedback("bad", json?.message || "인증번호 전송에 실패했습니다.");
                return;
            }

            const data = json.data || {};
            const otpExpires = Number(data.otpExpiresInSeconds ?? 300);
            const cooldown = Number(data.resendCooldownSeconds ?? 60);

            if (otpInput) otpInput.disabled = false;
            if (otpVerifyBtn) otpVerifyBtn.disabled = false;
            if (otpResendBtn) otpResendBtn.disabled = true;

            otpInput?.focus();

            setOtpFeedback("ok", json.message || "인증번호를 전송했습니다.");
            startOtpCountdown(otpExpires);
            startResendCooldown(cooldown);
        } catch (e) {
            showGlobalError("현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            setOtpFeedback("", "");
        } finally {
            emailProofState.sending = false;
            updateEmailVerifyBtnState();
        }
    }

    async function verifyOtp() {
        clearGlobalError();

        if (emailProofState.verifying || emailProofState.verified) return;

        const emailValue = (configs.find((c) => c.name === "email")?.input?.value ?? "").trim();
        const otpValue = (otpInput?.value ?? "").trim();

        const localErr = validateOtpLocal(otpValue);
        if (localErr) {
            if (otpInput) {
                otpInput.classList.remove("is-valid");
                otpInput.classList.add("is-invalid");
            }
            setOtpFeedback("bad", localErr);
            return;
        }

        emailProofState.verifying = true;
        if (otpVerifyBtn) otpVerifyBtn.disabled = true;
        setOtpFeedback("info", "검증 중...");

        try {
            const { res, json } = await postJson(OTP_VERIFY_URL, { email: emailValue, otp: otpValue });

            if (!res.ok && (res.status === 503 || res.status >= 500)) {
                showGlobalError(json?.message);
                setOtpFeedback("", "");
                return;
            }

            if (!res.ok || !json || json.success === false) {
                const msg = json?.message || "OTP 인증에 실패했습니다.";
                if (otpInput) {
                    otpInput.classList.remove("is-valid");
                    otpInput.classList.add("is-invalid");
                }
                setOtpFeedback("bad", msg);
                return;
            }

            emailProofState.verified = true;
            clearOtpIntervals();

            if (otpInput) {
                otpInput.classList.remove("is-invalid");
                otpInput.classList.add("is-valid");
                otpInput.disabled = true;
            }

            if (otpVerifyBtn) otpVerifyBtn.disabled = true;
            if (otpResendBtn) otpResendBtn.disabled = true;

            if (emailVerifyBtn) {
                emailVerifyBtn.disabled = true;
                emailVerifyBtn.textContent = "인증 완료";
            }

            setOtpFeedback("ok", json.message || "이메일 인증이 완료되었습니다.");
        } catch (e) {
            showGlobalError("현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            setOtpFeedback("", "");
        } finally {
            emailProofState.verifying = false;
            if (!emailProofState.verified && otpVerifyBtn) otpVerifyBtn.disabled = false;
        }
    }

    // ---------------- validation pipeline ----------------
    function validateRequired(cfg, v) {
        const msg = readAttr(cfg, "requiredMsg", "필수 입력입니다.");
        return v ? null : msg;
    }

    function validateSize(cfg, v) {
        const minStr = readAttr(cfg, "min", null);
        const maxStr = readAttr(cfg, "max", null);
        const msg = readAttr(cfg, "sizeMsg", "길이가 올바르지 않습니다.");
        if (!minStr && !maxStr) return null;

        const min = minStr ? Number(minStr) : null;
        const max = maxStr ? Number(maxStr) : null;

        if (Number.isFinite(min) && v.length < min) return msg;
        if (Number.isFinite(max) && v.length > max) return msg;
        return null;
    }

    function validateFormat(cfg, v) {
        const regexStr = readAttr(cfg, "regex", null);
        if (!regexStr) return null;

        const msg = readAttr(cfg, "formatMsg", "형식이 올바르지 않습니다.");
        return new RegExp(regexStr).test(v) ? null : msg;
    }

    async function checkAvailability(cfg, v) {
        const st = availabilityState[cfg.name];
        if (!st) return { ok: true, reason: "" };

        if (st.last === v) return { ok: st.ok, reason: st.reason || "" };

        if (st.controller) st.controller.abort();
        st.controller = new AbortController();

        const urlTemplate = readAttr(cfg, "checkUrl", null);
        if (!urlTemplate) return { ok: true, reason: "" };

        const url = urlTemplate.replace("{value}", encodeURIComponent(v));

        const res = await fetch(url, {
            method: "GET",
            credentials: "same-origin",
            headers: { Accept: "application/json" },
            signal: st.controller.signal
        });

        let json = null;
        try {
            json = await res.json();
        } catch (_) {}

        // 5xx/503 -> global
        if (!res.ok && (res.status === 503 || res.status >= 500)) {
            showGlobalError(json?.message);
            st.last = v;
            st.ok = false;
            st.reason = "현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.";
            return { ok: false, reason: st.reason };
        }

        // 4xx -> field 아래
        if (!res.ok) {
            const msg = (json?.message ?? json?.detail ?? "값이 올바르지 않습니다.").toString();
            st.last = v;
            st.ok = false;
            st.reason = msg;
            return { ok: false, reason: msg };
        }
        clearGlobalError();

        if (!json) return { ok: false, reason: "확인할 수 없습니다." };
        if (json.success === false) return { ok: false, reason: json.message || "확인할 수 없습니다." };

        const data = json.data || {};
        const ok = Boolean(data.available);
        const reason = (data.reason ?? "").toString();

        st.last = v;
        st.ok = ok;
        st.reason = reason;

        return { ok, reason };
    }

    async function validateOnBlur(cfg) {
        const v = valueOf(cfg);

        const requiredError = validateRequired(cfg, v);
        if (requiredError) {
            setInputState(cfg, "is-invalid");
            setFeedback(cfg, "bad", requiredError);
            if (cfg.name === "email") updateEmailVerifyBtnState();
            return false;
        }

        const sizeError = validateSize(cfg, v);
        if (sizeError) {
            setInputState(cfg, "is-invalid");
            setFeedback(cfg, "bad", sizeError);
            if (cfg.name === "email") updateEmailVerifyBtnState();
            return false;
        }

        const formatError = validateFormat(cfg, v);
        if (formatError) {
            setInputState(cfg, "is-invalid");
            setFeedback(cfg, "bad", formatError);
            if (cfg.name === "email") updateEmailVerifyBtnState();
            return false;
        }

        if (cfg.availability) {
            setInputState(cfg, null);

            const spinnerHtml = `<span class="spinner-border spinner-border-sm align-middle" role="status" aria-hidden="true"></span>`;

            const loadingTimer = setTimeout(() => {
                setFeedbackHtml(cfg, "info", spinnerHtml);
            }, 250);

            try {
                const r = await checkAvailability(cfg, v);
                clearTimeout(loadingTimer);

                if (r.ok) {
                    setInputState(cfg, "is-valid");
                    setFeedback(cfg, "ok", r.reason);
                } else {
                    setInputState(cfg, "is-invalid");
                    setFeedback(cfg, "bad", r.reason || "");
                }

                if (cfg.name === "email") updateEmailVerifyBtnState();
                return r.ok;
            } catch (e) {
                clearTimeout(loadingTimer);
                if (e?.name === "AbortError") return false;
                setInputState(cfg, "is-invalid");
                setFeedback(cfg, "bad", "확인 중 오류가 발생했습니다.");
                if (cfg.name === "email") updateEmailVerifyBtnState();
                return false;
            }
        }

        setInputState(cfg, "is-valid");
        setFeedback(cfg, "ok", "사용 가능한 형식입니다.");
        if (cfg.name === "email") updateEmailVerifyBtnState();
        return true;
    }

    function resetFieldOnInput(cfg) {
        setInputState(cfg, null);
        setFeedback(cfg, "", "");
        if (cfg.feedback) cfg.feedback.innerHTML = "";

        if (cfg.availability) {
            const st = availabilityState[cfg.name];
            if (st) {
                st.ok = false;
                st.last = null;
                st.reason = "";
                if (st.controller) {
                    try {
                        st.controller.abort();
                    } catch (_) {}
                    st.controller = null;
                }
            }
        }

        if (cfg.name === "email") {
            resetEmailProofUi();
            updateEmailVerifyBtnState();
        }
    }

    // ---------------- 가입하기(POST /signup JSON) ----------------
    function resetEmailProofAndGuideEmail(msg) {
        resetEmailProofUi();
        emailProofState.verified = false;
        applyFieldError("email", msg);
        updateEmailVerifyBtnState();
    }

    function resetEmailProofAndGuideOtp(msg) {
        resetEmailProofUi();
        emailProofState.verified = false;
        setOtpFeedback("bad", msg);
        updateEmailVerifyBtnState();
    }

    async function submitSignupAjax() {
        clearGlobalError();

        const payload = {
            username: (byId("username")?.value ?? "").trim(),
            password: byId("password")?.value ?? "",
            email: (byId("email")?.value ?? "").trim(),
            nickname: (byId("nickname")?.value ?? "").trim()
        };

        const res = await fetch(SIGNUP_URL, {
            method: "POST",
            credentials: "same-origin",
            headers: { Accept: "application/json", "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        let json = null;
        try {
            json = await res.json();
        } catch (_) {}

        if (!res.ok && (res.status === 503 || res.status >= 500)) {
            showGlobalError(json?.message);
            return;
        }

        if (res.ok && json?.success === true) {
            window.location.assign("/");
            return;
        }

        const code = json?.code;
        const msg = json?.message || "요청에 실패했습니다.";

        if (code === CODES.USERNAME_DUP) {
            availabilityState.username.ok = false;
            applyFieldError("username", msg);
            return;
        }
        if (code === CODES.EMAIL_DUP) {
            availabilityState.email.ok = false;
            resetEmailProofUi();
            applyFieldError("email", msg);
            updateEmailVerifyBtnState();
            return;
        }
        if (code === CODES.NICK_DUP) {
            availabilityState.nickname.ok = false;
            applyFieldError("nickname", msg);
            return;
        }

        if (code === CODES.EMAIL_MISMATCH) {
            resetEmailProofAndGuideEmail(msg);
            return;
        }

        // PROOF_EXPIRED 까지만
        if (code === CODES.PROOF_EXPIRED) {
            resetEmailProofAndGuideOtp(msg);
            return;
        }

        showGlobalError(msg);
    }

    // ---------------- bind blur/input ----------------
    configs.forEach((cfg) => {
        cfg.input?.addEventListener("blur", () => validateOnBlur(cfg));
        cfg.input?.addEventListener("input", () => resetFieldOnInput(cfg));
    });

    // OTP handlers
    emailVerifyBtn?.addEventListener("click", sendEmailVerification);
    otpVerifyBtn?.addEventListener("click", verifyOtp);
    otpResendBtn?.addEventListener("click", sendEmailVerification);

    // 핵심: 어떤 경우에도 기본 submit 차단 + 검증 통과 시에만 AJAX
    form?.addEventListener(
        "submit",
        async (e) => {
            e.preventDefault(); // 무조건 기본 submit 차단 (Enter/버튼 모두)
            e.stopPropagation();
            clearGlobalError();

            // 전체 필드 검증(필수/길이/정규식/중복검사)
            const results = await Promise.all(configs.map((cfg) => validateOnBlur(cfg)));
            const allOk = results.every(Boolean);

            const avOk =
                availabilityState.username.ok &&
                availabilityState.email.ok &&
                availabilityState.nickname.ok;

            if (!allOk || !avOk) return;

            // OTP 인증 여부
            if (!emailProofState.verified) {
                setOtpFeedback("bad", "이메일 인증을 완료해주세요.");
                if (otpInput) {
                    otpInput.disabled = false;
                    otpInput.focus();
                }
                return;
            }

            // 여기서만 가입 요청
            await submitSignupAjax();
        },
        true // 캡처 단계: 기본 submit/다른 핸들러보다 먼저 잡음
    );

    // password toggle
    const toggleBtn = byId("togglePassword");
    const pw = byId("password");
    const icon = byId("togglePasswordIcon");

    toggleBtn?.addEventListener("click", () => {
        if (!pw || !icon) return;

        const willShow = pw.type === "password";
        pw.type = willShow ? "text" : "password";
        icon.classList.toggle("bi-eye", willShow);
        icon.classList.toggle("bi-eye-slash", !willShow);
    });

    // init
    resetEmailProofUi();
    updateEmailVerifyBtnState();
    renderTimers();
})();
