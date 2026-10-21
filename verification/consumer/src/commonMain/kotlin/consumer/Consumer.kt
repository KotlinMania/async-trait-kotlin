package consumer

import io.github.kotlinmania.asynctrait.Args
import io.github.kotlinmania.asynctrait.InferredBound
import io.github.kotlinmania.asynctrait.TokenStreamParseResult
import io.github.kotlinmania.asynctrait.asyncTrait
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote

fun expand(): TokenStreamParseResult =
    asyncTrait(quote(""), quote("trait Consumer { async fn run(&self); }"))

fun inferredBound(): ToTokens = InferredBound.Send

fun options(): Args = Args(local = false)
