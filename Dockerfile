# syntax=docker/dockerfile:1
#
# docker build --target development -t brijeshdhaker/php:8.4.13 -f Dockerfile .
# docker build --target production -t brijeshdhaker/php:8.4.13 -f Dockerfile .
#

## FROM composer:lts as prod-deps
## WORKDIR /app
## RUN --mount=type=bind,source=./composer.json,target=composer.json \
##     --mount=type=bind,source=./composer.lock,target=composer.lock \
##     --mount=type=cache,target=/tmp/cache \
##     composer install --no-dev --no-interaction

## FROM composer:lts as dev-deps
## WORKDIR /app
## RUN --mount=type=bind,source=./composer.json,target=composer.json \
##     --mount=type=bind,source=./composer.lock,target=composer.lock \
##     --mount=type=cache,target=/tmp/cache \
##     composer install --no-interaction

#
# BASE STAGE
#
FROM mcr.microsoft.com/devcontainers/php:1-8.4-apache-bullseye AS base
# Install necessary packages
RUN apt-get update && \
    apt-get install -y \
    libzip-dev libssh2-1-dev libmemcached-dev libssl-dev wget git unzip \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Install PHP Extensions
RUN docker-php-ext-install zip pdo pdo_mysql 

# Install the ssh2-1.4 extension using pecl and enable it
RUN pecl install -o -f ssh2-1.4 memcached \
    && docker-php-ext-enable ssh2 memcached

RUN a2enmod rewrite
RUN a2enmod actions
#
ARG USERNAME=brijeshdhaker
ARG USER_UID=2000
ARG USER_GID=$USER_UID

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    #
    # [Optional] Add sudo support. Omit if you don't need to install software after connecting.
    && apt-get update \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

# ********************************************************
# * Anything else you want to do like clean up goes here *
# ********************************************************

# [Optional] Set the default user. Omit if you want to keep the default as root.
# USER $USERNAME
WORKDIR /var/www/html


#
# DEVELOPMENT STAGE
#
FROM base AS development
# Install the Xdebug extension using pecl and enable it
#RUN pecl install -o -f xdebug  && docker-php-ext-enable xdebug
COPY conf/php/php.ini "$PHP_INI_DIR/php.ini" 
#RUN mv "$PHP_INI_DIR/php.ini-development" "$PHP_INI_DIR/php.ini"
#COPY --from=dev-deps app/vendor/ /var/www/html/vendor
USER www-data

#
# PRODUCTION STAGE
#
FROM base AS production
RUN mv "$PHP_INI_DIR/php.ini-production" "$PHP_INI_DIR/php.ini"
#COPY --from=prod-deps app/vendor/ /var/www/html/vendor
USER www-data
