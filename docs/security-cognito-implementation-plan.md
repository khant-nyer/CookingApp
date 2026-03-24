# Cognito Security Implementation Plan

## Scope
Implement backend support for:
1. User registration (`email`, `userName`, `password`) through Amazon Cognito.
2. Local user persistence with Cognito identity (`sub`) and profile metadata.
3. Access-token validation on every protected backend request.

Login/logout stay on the frontend and are not handled by backend endpoints.

## Assumptions
- Frontend sends `Authorization: Bearer <access_token>` for protected APIs.
- Backend uses Cognito User Pool JWTs (RS256 + JWKS).
- Existing `User` table is the app-level profile store.

## Phase 1: Data model updates
1. Extend `User` entity with:
   - `cognitoSub` (required, unique, immutable in app flow)
   - `profileImageUrl` (optional)
2. Add DB constraints/indexes:
   - unique index on `cognito_sub`
   - optional unique index on `email` (business decision)
3. Extend repository queries:
   - `findByCognitoSub(String sub)`
   - `findByEmail(String email)`

## Phase 2: Configuration and dependencies
1. Add Spring Security + OAuth2 resource server dependencies.
2. Add configuration properties:
   - `security.cognito.region`
   - `security.cognito.userPoolId`
   - `security.cognito.appClientId`
   - derived issuer/JWKS URL
3. Keep env-specific values in dedicated `.env` files (for example, `.env.dev` and `.env.prod`) while using a single `application.properties`.

## Phase 3: Registration API
1. Add endpoint `POST /api/auth/register`.
2. Request payload:
   - `email`
   - `userName`
   - `password`
   - optional `profileImageUrl`
3. Service flow:
   - Validate request
   - Create user in Cognito (SignUp/AdminCreateUser flow)
   - Obtain `sub`
   - Persist local user (`cognitoSub`, `email`, `userName`, `profileImageUrl`)
   - Return sanitized user response DTO
4. Error handling mapping:
   - Cognito username/email exists -> `409 Conflict`
   - weak password/validation -> `400 Bad Request`
   - AWS transient errors -> `503 Service Unavailable`

## Phase 4: JWT validation for protected APIs
1. Configure `SecurityFilterChain`:
   - permit `/api/auth/register` and health/docs endpoints
   - require authentication for all other `/api/**`
2. Configure JWT decoder from Cognito issuer/JWKS.
3. Add claim validators:
   - `iss` matches pool issuer
   - `exp` valid
   - `token_use` is `access`
   - audience/client check according to chosen token format
4. Map principal to app identity by `sub` claim.

## Phase 5: Authorization and ownership model (incremental)
1. For user-owned resources, resolve current user by `sub`.
2. Enforce ownership checks in service layer.
3. Optionally map Cognito groups to roles later.

## Phase 6: Testing strategy
1. Unit tests:
   - registration service happy path + duplicate + weak password
   - JWT claim validator behavior
2. Integration tests:
   - unauthenticated request to protected endpoint returns 401
   - valid token accepted
   - invalid issuer/audience/token_use rejected
3. Contract tests for `/api/auth/register` payload and error shape.

## Information needed from Cognito before coding full flow
1. AWS region
2. User Pool ID
3. App Client ID
4. Token type backend should accept (recommended: access token)
5. Signup mode (self signup vs admin create)
6. Required attributes/custom attributes
7. Environment-specific pool/client values (dev/prod)

## Rollout order
1. Merge data-model changes first (non-breaking).
2. Merge JWT validation in monitor mode for dev testing.
3. Enable endpoint protection for all non-auth API paths.
4. Release registration endpoint and frontend integration.


## Environment values provided
- region: `ap-southeast-2`
- user pool id: `ap-southeast-2_iesyw1kMl`
- app client id: `2v36scmicr5rqoqio57g4hnqcv`
- API token expectation: access token only
- signup confirmation: email verification required
- groups: `admin`, `user`

## Delivery in current PR
- Phase 1 implemented
- Phase 2 implemented
- Phase 3 implemented
- Phase 4 implemented
- Phase 5 implemented
- Phase 6 implemented
