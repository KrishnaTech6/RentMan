# Errors are values, caught once in the data layer

Repository implementations catch infrastructure exceptions in one place (`safeFlow` and `safeCall`) and return them as `Outcome.Failure(DomainError)`. Use cases pass `Outcome` through and ViewModels map it to `UiState` with `fold`, with no `try/catch` or `.catch` anywhere above the data layer. We rejected a `.catch` in every ViewModel pipeline: it's repeated in every screen, and a `.catch` at the end of a chain ends the stream, so Retry could never work. The cost is that every read type is wrapped in `Outcome`.
