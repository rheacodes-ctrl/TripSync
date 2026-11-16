# REST contract expected from Spring Boot (JSON, camelCase)

| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/login | `{email,password}` -> `{user,token}` |
| POST | /api/auth/register | `{name,email,password,phone,role}` -> `{user,token}` |
| GET | /api/users/{id}/trips | trips + `memberCount,destinationCount,spent` |
| GET | /api/trips/{id} | full trip: `admin, members[], destinations[].activities[] (participantIds, voterIds), expenses[].splits[], reservations[], payments[], documents[]` |
| POST/PUT/DELETE | /api/trips, /api/trips/{id} | |
| POST / DELETE | /api/trips/{id}/members, /members/{userId} | add body `{email}` |
| POST | /api/trips/{id}/destinations; DELETE /api/destinations/{id} | |
| POST | /api/destinations/{id}/activities | |
| PATCH | /api/activities/{id}/status | `{status}`; DELETE /api/activities/{id} |
| POST | /api/activities/{id}/votes, /participants | `{userId}` toggles on/off |
| POST | /api/trips/{id}/reservations; PATCH /api/reservations/{id}/status; DELETE | field `type` = HOTEL/TRANSPORT/ACTIVITY |
| POST | /api/trips/{id}/expenses | `{description,amount,paidBy,expenseDate,participantIds}`; server splits equally |
| DELETE | /api/expenses/{id} | |
| POST | /api/trips/{id}/payments | `{fromUserId,toUserId,amount}` |
| POST/DELETE | /api/trips/{id}/documents, /api/documents/{id} | switch to multipart upload |

Auth: `Authorization: Bearer <token>` header is already sent.
