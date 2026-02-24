# Phase 2 Readiness Audit (Mainline Snapshot)

## Scope
This audit checks whether the repository behavior currently matches **Phase 2** goals before starting Phase 3.

## Verdict
**Phase 2 is mostly implemented and functionally aligned on core backend behavior**, with one notable API-contract gap:

- ✅ Discovery is city-dependent and no longer falls back to user profile city.
- ✅ Unknown/non-verified cities no longer reuse Bangkok fallback results.
- ✅ Discovery data source is DB-only for verified city rows.
- ⚠️ For unknown cities, current behavior is a `BusinessException` (HTTP 400) rather than a structured success payload (`200 + empty + metadata`) or explicit status enum payload.

## Evidence summary

1. **Controller requires `city` and no longer accepts `userId`**
   - Discovery endpoints only pass `ingredientName` + `city` to service.

2. **Service signatures removed `userId` from the discovery flow**
   - `IngredientService -> IngredientDiscoveryFacade -> SupermarketDiscoveryService` all use `(city, ingredientName)`.

3. **No city fallback-to-user behavior in discovery service**
   - `resolveCity` validates non-empty city only.
   - No user lookup in discovery path.

4. **Unknown city semantics changed away from fallback seeds**
   - If no persisted city supermarkets exist, service throws:
     - `No verified supermarkets found for city: <city>`
   - This prevents Bangkok/global fallback response for unrelated cities.

5. **Discovery source semantics reserved for DB-backed rows**
   - Returned records use `discoverySource = "DB"`.

6. **User model no longer stores city**
   - `User` entity has no `city` field.

7. **Fallback seed config still exists but appears unused by current discovery service**
   - `application.properties` still includes fallback seed properties.
   - `SupermarketDiscoveryProperties` still models fallback markets.
   - This is not a behavior bug today, but can confuse future maintenance.

## Recommendation before Phase 3
- Decide and lock the Phase 2 API semantics for unknown city:
  - keep current `400 BusinessException`, **or**
  - migrate to `200 + [] + metadata/status` (`NO_VERIFIED_SUPERMARKETS_FOR_CITY`).
- If keeping `400`, update plan docs to reflect this chosen contract.
- Optionally remove dead fallback properties/classes now (or explicitly mark deprecated) to reduce ambiguity before introducing a City Discovery Provider in Phase 3.
