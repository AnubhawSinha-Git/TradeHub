#!/usr/bin/env bash
# Starts the TradeHub backend with the saved JWT secret.
cd "$(dirname "$0")/backend"
export JWT_SECRET=$(grep '^JWT_SECRET=' "$HOME/.tradehub_jwt_secret" | cut -d= -f2-)
exec mvn spring-boot:run "$@"