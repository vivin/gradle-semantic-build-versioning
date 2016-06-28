GRADLE_PROPERTIES="$HOME/.gradle/gradle.properties"

if [ ! -f "$GRADLE_PROPERTIES" ]; then
    touch $GRADLE_PROPERTIES
    echo "gradle.publish.key=$PUBLISH_KEY" >> $GRADLE_PROPERTIES
    echo "gradle.publish.secret=$PUBLISH_SECRET" >> $GRADLE_PROPERTIES
fi

