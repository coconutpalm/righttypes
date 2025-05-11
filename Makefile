jar:
	clojure -X:jar :version '"0.7.1"'

deploy:
	env CLOJARS_USERNAME=coconutpalm CLOJARS_PASSWORD=$CLOJARS_PASSWORD clj -X:deploy

ALL: jar deploy
