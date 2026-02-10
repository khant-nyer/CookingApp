# API Example Payloads

## Ingredient payloads for tiramisu

- `tiramisu-ingredients.json` is an **array payload**.
- Use it with `POST /api/ingredients/bulk`.

If you send this array to `POST /api/ingredients`, Spring will throw:

`Cannot deserialize value of type IngredientDTO from Array value (JsonToken.START_ARRAY)`

because `/api/ingredients` accepts a **single JSON object** (`IngredientDTO`), not a JSON array.

## Recipe payload for tiramisu

- `tiramisu-recipe.json` is a single JSON object.
- Use it with `POST /api/recipes` (or `/api/recipes/foods/{foodId}`).

> Replace `ingredientId` values with IDs returned from ingredient creation in your own environment.
