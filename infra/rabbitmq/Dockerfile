FROM rabbitmq:3.13-management
ADD --chmod=0644 https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.13.0/rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/
RUN rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange
COPY --chmod=0755 seed-credentials.sh /usr/local/bin/seed-credentials
ENTRYPOINT ["seed-credentials"]
CMD ["rabbitmq-server"]
