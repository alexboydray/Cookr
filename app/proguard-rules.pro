# Keep kotlinx.serialization generated serializers for the data models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.alexray.cookr.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.alexray.cookr.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
