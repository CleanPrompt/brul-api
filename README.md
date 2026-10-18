brul-api/
│
├── module.yaml
│
├── resources/
│   └── application.yaml
│
├── src/
│   └── nl/
│       └── brul/
│           └── api/
│               │
│               ├── Application.kt
│               ├── Routing.kt
│               │
│               ├── config/
│               │   └── AppConfig.kt
│               │
│               ├── database/
│               │   ├── DatabaseFactory.kt
│               │   │
│               │   └── tables/
│               │       ├── UsersTable.kt
│               │       ├── RefreshSessionsTable.kt
│               │       ├── ReportsTable.kt
│               │       ├── VotesTable.kt
│               │       └── HelpParticipationsTable.kt
│               │
│               ├── common/
│               │   └── error/
│               │       ├── ApiError.kt
│               │       ├── AppException.kt
│               │       └── ErrorHandling.kt
│               │
│               ├── security/
│               │   ├── PasswordHasher.kt
│               │   ├── BCryptPasswordHasher.kt
│               │   ├── JwtService.kt
│               │   ├── JwtAuthentication.kt
│               │   └── RefreshTokenService.kt
│               │
│               └── features/
│                   │
│                   ├── user/
│                   │   ├── UserRoutes.kt
│                   │   │
│                   │   ├── model/
│                   │   │   ├── User.kt
│                   │   │   └── Role.kt
│                   │   │
│                   │   ├── dto/
│                   │   │   └── UserResponse.kt
│                   │   │
│                   │   └── repository/
│                   │       ├── UserRepository.kt
│                   │       └── ExposedUserRepository.kt
│                   │
│                   ├── auth/
│                   │   ├── AuthRoutes.kt
│                   │   ├── AuthService.kt
│                   │   │
│                   │   ├── dto/
│                   │   │   ├── RegisterRequest.kt
│                   │   │   ├── LoginRequest.kt
│                   │   │   ├── LoginResponse.kt
│                   │   │   ├── RefreshRequest.kt
│                   │   │   └── AccessTokenResponse.kt
│                   │   │
│                   │   └── repository/
│                   │       ├── RefreshSessionRepository.kt
│                   │       └── ExposedRefreshSessionRepository.kt
│                   │
│                   └── report/
│                       └── model/
│                           ├── Report.kt
│                           ├── ReportType.kt
│                           ├── ReportCategory.kt
│                           ├── Location.kt
│                           ├── SafetyReport.kt
│                           ├── NuisanceReport.kt
│                           ├── HelpRequest.kt
│                           ├── HelpStatus.kt
│                           └── HelpParticipation.kt
│
└── http/
├── 01-health.http
├── 02-register.http
├── 03-auth.http
└── 04-jwt-auth.http