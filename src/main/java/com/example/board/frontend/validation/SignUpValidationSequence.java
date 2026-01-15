package com.example.board.frontend.validation;

import jakarta.validation.GroupSequence;

@GroupSequence({NotBlankGroup.class, SizeGroup.class, FormatGroup.class})
public interface SignUpValidationSequence {
}
