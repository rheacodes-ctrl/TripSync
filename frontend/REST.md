# REST contract expected from Spring Boot (JSON, camelCase)

| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/login | `{email,password}` -> `{user,token}` |
| POST | /api/auth/register | `{name,email,password,phone,role}` -> `{user,token}` |
| PUT | /api/users/{id} | profile: `{name,phone,preferences[],notifyVia[]}` (`notifyVia` = `EMAIL` / `WHATSAPP`) -> updated user |
| GET | /api/users/{id}/trips | trips + `memberCount,destinationCount,spent,members[]` |
| GET | /api/trips/{id} | full trip: `admin, members[], destinations[].activities[] (participantIds, voterIds), expenses[].splits[], reservations[], payments[], documents[]` |
| POST/PUT/DELETE | /api/trips, /api/trips/{id} | |
| POST / DELETE | /api/trips/{id}/members, /members/{userId} | add body `{email}` |
| POST | /api/trips/{id}/destinations; DELETE /api/destinations/{id} | dates must be inside the trip dates (400 otherwise) |
| POST | /api/destinations/{id}/activities | `activityDate` must be inside the destination stay (400 otherwise) |
| PATCH | /api/activities/{id}/status | `{status}`; DELETE /api/activities/{id} |
| POST | /api/activities/{id}/votes, /participants | `{userId}` toggles on/off |
| POST | /api/trips/{id}/reservations; PATCH /api/reservations/{id}/status; DELETE | `type` = HOTEL/TRANSPORT/ACTIVITY; type-specific fields go in `details` (JSON column): hotel `{checkInDate,checkOutDate,numberOfRooms}`, transport `{transportType,departureLocation,arrivalLocation,departureTime,bookingReference}`, activity `{activityName}`. Return 400 `BudgetExceededException: ...` if the cost would exceed the trip budget |
| POST | /api/trips/{id}/expenses | `{description,amount,paidBy,expenseDate,splitType}` + `participantIds[]` when `EQUAL` (server splits) or `splits:[{userId,shareAmount}]` when `CUSTOM` (must sum to `amount`). 400 `BudgetExceededException: ...` over budget |
| DELETE | /api/expenses/{id} | |
| POST | /api/trips/{id}/payments | `{fromUserId,toUserId,amount}` |
| POST | /api/trips/{id}/notifications | `{type,userIds[],message}`; type = INVITATION / RESERVATION_UPDATE / EXPENSE_REMINDER / TRIP_UPDATE. Backend sends via Gmail and/or WhatsApp per user's `notifyVia` and logs one row per recipient per channel `{notificationId,type,channel,toUserId,message,status,sentAt}`. Also trigger automatically on add-member (INVITATION) and reservation status change (RESERVATION_UPDATE) |
| POST/DELETE | /api/trips/{id}/documents, /api/documents/{id} | switch to multipart upload |

Auth: `Authorization: Bearer <token>` header is already sent.

**Errors:** return a non-2xx status with a plain-text body. The UI shows that text as-is, and gives text starting with `BudgetExceededException` a dedicated warning style.
