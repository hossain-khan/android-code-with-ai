package dev.hossain.codematex.di

import android.app.Activity
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/**
 * A Metro map key annotation used for registering an [Activity] into the dependency graph.
 *
 * This is used with [@ContributesIntoMap][dev.zacsweers.metro.ContributesIntoMap] to add
 * Activities to the multibinding map that [ComposeAppComponentFactory] uses for
 * constructor injection of Activities.
 *
 * Because `implicitClassKey = true` is enabled, the Activity class is inferred automatically
 * by the Metro compiler when [value] is omitted.
 *
 * Example usage:
 * ```kotlin
 * @ActivityKey
 * @ContributesIntoMap(AppScope::class, binding = binding<Activity>())
 * class MainActivity(
 *     private val circuit: Circuit,
 * ) : ComponentActivity() {
 *     // ...
 * }
 * ```
 *
 * See https://zacsweers.github.io/metro/latest/bindings/#multibindings for more on map multibindings.
 * See [MapKey] for more on map key annotations.
 */
@MapKey(implicitClassKey = true)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ActivityKey(
    val value: KClass<out Activity> = Nothing::class,
)
