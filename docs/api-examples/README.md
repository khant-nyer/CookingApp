# API Example Payloads

## Ingredient payloads for tiramisu

- `tiramisu-ingredients.json` is an **array payload**.
- Use it with `POST /api/ingredients/bulk`.

If you send this array to `POST /api/ingredients`, Spring will throw:

`Cannot deserialize value of type IngredientDTO from Array value (JsonToken.START_ARRAY)`

because `/api/ingredients` accepts a **single JSON object** (`IngredientDTO`), not a JSON array.

### Bulk endpoint behavior

- Bulk creation is **all-or-nothing** in one transaction.
- Duplicate names inside the same array are rejected.
- If any ingredient already exists (name, case-insensitive), the request is rejected.

## Recipe payload for tiramisu

- `tiramisu-recipe.json` is a single JSON object.
- Use it with `POST /api/recipes` (or `/api/recipes/foods/{foodId}`).

> Replace `ingredientId` values with IDs returned from ingredient creation in your own environment.

## OpenAPI docs

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
