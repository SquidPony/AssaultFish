AssaultFish
===========

Assault Fish is my entry for the 2014 7dRL Challenge

Build Notes
-----------

- Java 25 is required for local builds.
- The Gradle wrapper in `Remake` is configured for Gradle 9.1.0.
- Legacy repositories were removed in favor of `mavenCentral()` and explicit dependency sources.
- The legacy HTML/GWT module is opt-in; include it with `-PincludeHtml=true` when needed.
