all:

install:
	npm i

clean:
	rm -rf .jest .shadow-cljs .cpcache dist

lint: install
	clojure -A:fmt -M -m nsorg.cli src
	clojure -A:fmt -M -m cljfmt.main check src
	./node_modules/.bin/eslint .
	./node_modules/.bin/prettier --check .

lint-fix: install
	clojure -A:fmt -M -m nsorg.cli --replace src
	clojure -A:fmt -M -m cljfmt.main fix src
	./node_modules/.bin/eslint --fix .
	./node_modules/.bin/prettier -w .

build: clean
	clojure -A:dev -X cljest.build/main

analyze: build
	clojure -A:dev -X cljest.analyze/main

install-local-jar: build
	clj -A:dev -X cljest.deploy/main

publish-to-clojars: build
	clj -A:dev -X cljest.deploy/main :clojars? true

server:
	clojure -A:test -X cljest.compilation/watch

compile:
	clojure -A:test -X cljest.compilation/compile

watch: install
	./node_modules/.bin/jest --watch

jest-ci: install compile
	CI=true ./node_modules/.bin/jest --ci

repl:
	clj -J-Dclojure.server.repl="{:port 5678 :accept clojure.core.server/repl}"
