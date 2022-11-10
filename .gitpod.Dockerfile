FROM gitpod/workspace-java-17

ENV TRIGGER_REBUILD=1
ARG YB_VERSION=2.15.3.0
ARG YB_BUILD=231
ARG YB_BIN_PATH=/usr/local/yugabyte
ARG ROLE=gitpod

USER $ROLE

# create bin and data path
RUN sudo mkdir -p $YB_BIN_PATH \
    && sudo mkdir -p /var/ybdp
# set permission
RUN sudo chown -R $ROLE:$ROLE /var/ybdp \
    && sudo chown -R $ROLE:$ROLE /usr/local/yugabyte


# fetch the binary
RUN curl -sSLo ./yugabyte.tar.gz https://downloads.yugabyte.com/releases/${YB_VERSION}/yugabyte-${YB_VERSION}-b${YB_BUILD}-linux-x86_64.tar.gz \
    && tar -xvf yugabyte.tar.gz -C $YB_BIN_PATH --strip-components=1 \
    && chmod +x $YB_BIN_PATH/bin/* \
    && rm ./yugabyte.tar.gz

# configure the interpreter
RUN ["/usr/local/yugabyte/bin/post_install.sh"]

# install Minio
RUN wget https://dl.min.io/server/minio/release/linux-amd64/minio \
    && chmod +x minio \
    && sudo mv minio /usr/local/bin

RUN sudo mkdir /usr/local/minio \
    && sudo chown -R $ROLE:$ROLE /usr/local/bin/minio \
    && sudo chown -R $ROLE:$ROLE /usr/local/minio  

# set the execution path and other env variables
ENV YB_STORE=/var/ybdp
ENV YSQL_PORT=5433
ENV YCQL_PORT=9042
ENV YB_WEB_PORT=7001
ENV TSERVER_WEB_PORT=9001
ENV YSQL_API_PORT=13000
ENV YCQL_API_PORT=12000

ENV MINIO_BIN_PATH=/usr/local/bin
ENV MINIO_CONSOLE_PORT=9005
ENV MINIO_STORE=/usr/local/minio
ENV MINIO_ROOT_USER=minio_user
ENV MINIO_ROOT_PASSWORD=password

ENV PATH="$YB_BIN_PATH/bin/:$MINIO_BIN_PATH:$PATH"

EXPOSE ${YSQL_PORT} ${YB_WEB_PORT} ${TSERVER_WEB_PORT} ${MINIO_CONSOLE_PORT}