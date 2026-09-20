# BudgetFlow keeps all financial data on-device; these rules only affect the
# optional release/minified build variant.

# Room entities/DAOs are referenced by generated code discovered via reflection-free
# annotation processing (KSP), so no special keep rules are required for them.

# kotlinx.serialization ships its own consumer rules for its generated serializers.
