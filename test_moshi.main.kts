
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.squareup.moshi:moshi:1.14.0")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.14.0")

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Request(val name: String, val age: Int? = null)

val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
val adapter = moshi.adapter(Request::class.java)

println(adapter.toJson(Request("Test", null)))

