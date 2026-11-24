# Room entities are separate from domain models

Entities live in `:data` as plain primitive classes and are mapped to domain models by internal mappers inside `:data` only. ViewModels and UI use the domain models directly, with no presentation models or mappers. We first planned no mappers at all, but putting Room annotations on domain models would couple `:domain` to Room and to the cross-module processing behavior of its compiler on iOS. The cost is one mapper pair per entity, each covered by a round-trip test.
