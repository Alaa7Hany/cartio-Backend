curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"email":"live_test@example.com", "password":"SecurePassword123", "fullName":"John Doe"}'