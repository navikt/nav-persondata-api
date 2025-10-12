package no.nav.persondataapi.rest.oppslag

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Maskerer alle felter annotert med @Maskert i et objekt og dets nestede objekter.
 *
 * @param obj Objektet som skal maskeres
 * @return En ny instans av objektet hvor alle @Maskert-annoterte felter er erstattet med maskert verdi
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> maskerObjekt(obj: T): T {
    val kClass = obj::class as KClass<T>
    
    // Håndter spesialtilfeller
    return when {
        // Primitiver og String-typer returneres direkte
        obj is String || obj is Number || obj is Boolean || obj is Enum<*> -> obj
        
        // Håndter lister
        obj is List<*> -> {
            obj.map { element ->
                element?.let { maskerObjekt(it as Any) }
            } as T
        }
        
        // Håndter maps
        obj is Map<*, *> -> {
            obj.mapValues { (_, value) ->
                value?.let { maskerObjekt(it as Any) }
            } as T
        }
        
        // Håndter data classes
        else -> maskerDataClass(obj, kClass)
    }
}

/**
 * Maskerer alle felter annotert med @Maskert i en data class.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Any> maskerDataClass(obj: T, kClass: KClass<T>): T {
    val properties = kClass.memberProperties
    val constructor = kClass.constructors.firstOrNull() ?: return obj
    
    val args = constructor.parameters.associateWith { param ->
        val property = properties.find { it.name == param.name } ?: return@associateWith null
        
        // Gjør property tilgjengelig hvis den ikke er det
        property.isAccessible = true
        
        val currentValue = property.get(obj)
        
        // Sjekk om feltet har @Maskert-annotasjon
        val maskertAnnotation = property.findAnnotation<Maskert>()
        
        if (maskertAnnotation != null && currentValue is String) {
            // Returner maskert verdi
            maskertAnnotation.maskertVerdi
        } else if (currentValue != null && !erPrimitiv(currentValue)) {
            // Rekursivt masker nestede objekter
            try {
                maskerObjekt(currentValue)
            } catch (e: Exception) {
                // Hvis maskering feiler, returner original verdi
                currentValue
            }
        } else {
            currentValue
        }
    }
    
    return try {
        constructor.callBy(args)
    } catch (e: Exception) {
        // Hvis konstruktørkall feiler, returner original objekt
        obj
    }
}

/**
 * Sjekker om en verdi er av primitiv type eller skal håndteres som primitiv.
 */
private fun erPrimitiv(value: Any): Boolean {
    return value is String ||
            value is Number ||
            value is Boolean ||
            value is Enum<*> ||
            value::class.java.isPrimitive
}
