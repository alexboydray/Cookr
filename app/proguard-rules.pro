# Keep kotlinx.serialization generated serializers for the data models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.alexray.recipeapp.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.alexray.recipeapp.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
