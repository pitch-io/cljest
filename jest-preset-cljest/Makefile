all:

install:
	npm i

lint: install
	./node_modules/.bin/eslint .
	./node_modules/.bin/prettier --check .

test:
	./node_modules/.bin/jest --watch

test-ci:
	./node_modules/.bin/jest --ci
