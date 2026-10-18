brul-api/
│
├── module.yaml
├── README.md
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
│               │   ├── error/
│               │   │   ├── ApiError.kt
│               │   │   ├── AppException.kt
│               │   │   └── ErrorHandling.kt
│               │   │
│               │   └── validation/
│               │       └── StrictJson.kt
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
│                   ├── report/
│                   │   ├── ReportRoutes.kt
│                   │   ├── ReportService.kt
│                   │   │
│                   │   ├── model/
│                   │   │   ├── Report.kt
│                   │   │   ├── ReportType.kt
│                   │   │   ├── ReportCategory.kt
│                   │   │   ├── Location.kt
│                   │   │   ├── SafetyReport.kt
│                   │   │   ├── NuisanceReport.kt
│                   │   │   ├── HelpRequest.kt
│                   │   │   ├── HelpStatus.kt
│                   │   │   └── HelpParticipation.kt
│                   │   │
│                   │   ├── dto/
│                   │   │   ├── CreateReportRequest.kt
│                   │   │   └── ReportResponse.kt
│                   │   │
│                   │   └── repository/
│                   │       ├── ReportRepository.kt
│                   │       └── ExposedReportRepository.kt
│                   │
│                   ├── vote/
│                   │   ├── VoteRoutes.kt
│                   │   ├── VoteService.kt
│                   │   │
│                   │   ├── dto/
│                   │   │   ├── VoteRequest.kt
│                   │   │   └── VoteResponse.kt
│                   │   │
│                   │   └── repository/
│                   │       ├── VoteRepository.kt
│                   │       └── ExposedVoteRepository.kt
│                   │
│                   └── help/
│                       ├── HelpRoutes.kt
│                       ├── HelpService.kt
│                       │
│                       ├── dto/
│                       │   ├── ParticipationResponse.kt
│                       │   └── ParticipantResponse.kt
│                       │
│                       └── repository/
│                           ├── HelpRepository.kt
│                           └── ExposedHelpRepository.kt
│
├── test/
│   └── nl/
│       └── brul/
│           └── api/
│               │
│               ├── features/
│               │   └── report/
│               │       ├── ReportServiceTest.kt
│               │       │
│               │       └── model/
│               │           ├── LocationTest.kt
│               │           └── HelpRequestTest.kt
│               │
│               └── integration/
│                   └── HelpDatabaseIntegrationTest.kt
│
└── http/