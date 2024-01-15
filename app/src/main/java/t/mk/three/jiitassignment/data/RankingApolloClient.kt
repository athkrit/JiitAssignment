package t.mk.three.jiitassignment.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.network.okHttpClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


class RankingApolloClient {
    private val apolloClient: ApolloClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)

        val cacheKeyGenerator = object : CacheKeyGenerator {
            override fun cacheKeyForObject(
                obj: Map<String, Any?>,
                context: CacheKeyGeneratorContext
            ): CacheKey? {
                return formatCacheKey(obj["id"] as? String?)
            }

            private fun formatCacheKey(id: String?): CacheKey? {
                return if (id.isNullOrEmpty()) {
                    null
                } else {
                    CacheKey(id)
                }
            }
        }

        ApolloClient.Builder()
            .serverUrl("https://asia-east2-jitta-api.cloudfunctions.net/graphqlDev")
            .okHttpClient(okHttpClient)
            .normalizedCache(
                normalizedCacheFactory = cacheFactory,
                cacheKeyGenerator = cacheKeyGenerator
            )
            .build()
    }

    suspend fun <D : Query.Data> query(
        query: Query<D>
    ): Result<D> {
        val response = apolloClient
            .query(query)
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

        val result = response.data?.let {
            Result.Success(it)
        } ?: Result.Error(response.errors?.firstOrNull()?.message ?: "unknown error")

        return result
    }

    sealed class Result<T : Any> {
        class Success<T : Any>(
            val response: T,
        ) : Result<T>()

        class Error<T : Any>(
            val error: String
        ) : Result<T>()
    }
}