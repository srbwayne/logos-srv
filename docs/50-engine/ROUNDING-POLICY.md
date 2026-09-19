# Rounding Policy

Status: **UNKNOWN / must be characterized**.

Current processing uses floating-point intermediate values and later integer/long conversions. That can imply truncation. The first engine characterization slice must capture the actual behavior in tests.

Do not replace `double`, introduce `BigDecimal`, or change conversion strategy as a cleanup-only refactor. Numeric semantics are domain behavior.
