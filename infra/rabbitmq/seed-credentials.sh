#!/bin/sh
set -eu

# Pass the literal UTF-8 bytes to Erlang when seeding a fresh broker.
binary_bytes() {
    printf '%s' "$1" | od -An -v -tu1 | awk '{
        for (i = 1; i <= NF; i++) {
            printf "%s%s", separator, $i
            separator = ","
        }
    }'
}

: "${RABBITMQ_DEFAULT_USER:?RabbitMQ username is required}"
: "${RABBITMQ_DEFAULT_PASS:?RabbitMQ password is required}"
umask 077
printf '[{rabbit,[{default_user,<<%s>>},{default_pass,<<%s>>}]}].\n' \
    "$(binary_bytes "$RABBITMQ_DEFAULT_USER")" "$(binary_bytes "$RABBITMQ_DEFAULT_PASS")" \
    > /etc/rabbitmq/advanced.config
chown rabbitmq:rabbitmq /etc/rabbitmq/advanced.config
unset RABBITMQ_DEFAULT_USER RABBITMQ_DEFAULT_PASS

exec docker-entrypoint.sh "$@"
