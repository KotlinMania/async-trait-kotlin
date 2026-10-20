import AsyncTrait
import IoGithubKotlinmaniaProcMacro2Kotlin
import Testing

@Suite("Async-trait exported API")
struct AsyncTraitTests {
    @Test func expandsARealTrait() {
        let result = expandAsyncTrait(arguments: "", source: "trait Service { async fn run(&self); }")
        #expect(result.isSuccess)
        #expect(result.error == nil)
        #expect(result.source?.contains("Future") == true)
        #expect(result.source?.contains("Send") == true)
    }

    @Test func rejectsMalformedArguments() {
        let result = expandAsyncTrait(arguments: "?", source: "trait Service {}")
        #expect(!result.isSuccess)
        #expect(result.source == nil)
        #expect(result.error?.contains("expected") == true)
    }

    @Test func rejectsMalformedTraitBody() {
        let result = expandAsyncTrait(
            arguments: "",
            source: "trait Trait { fn broken() { 1 + } }"
        )
        #expect(!result.isSuccess)
        #expect(result.source == nil)
        #expect(result.error?.isEmpty == false)
    }

    @Test func rejectsMalformedImplementationBody() {
        let result = expandAsyncTrait(
            arguments: "",
            source: "impl Trait for () { fn broken() { 1 + } }"
        )
        #expect(!result.isSuccess)
        #expect(result.source == nil)
        #expect(result.error?.isEmpty == false)
    }

    @Test func exposesConcreteArgumentValues() throws {
        let tokens = try #require(
            ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream.Companion.shared
                .fromString(src: "?Send").value
        )
        let result: ArgsParseResult = parseArgs(tokens: tokens)
        let args: Args = try #require(result.value)
        #expect(args.local)
        #expect(result.error == nil)
    }

    @Test func exposesConcreteTokenValues() throws {
        let args = try #require(
            ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream.Companion.shared
                .fromString(src: "").value
        )
        let source = try #require(
            ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream.Companion.shared
                .fromString(src: "trait Trait { async fn run(&self); }").value
        )
        let result = asyncTrait(args: args, input: source)
        let tokens: ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream =
            try #require(result.value)
        #expect(tokens.toString().contains("Future"))
        #expect(result.error == nil)
    }

    @Test func exposesConcreteDiagnosticMessages() throws {
        let tokens = try #require(
            ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream.Companion.shared
                .fromString(src: "?Sync").value
        )
        let result = parseArgs(tokens: tokens)
        let diagnostic = try #require(result.error)
        let message: String = diagnostic.message
        #expect(message.contains("expected"))
        #expect(result.value == nil)

        let source = try #require(
            ExportedKotlinPackages.io.github.kotlinmania.procmacro2.TokenStream.Companion.shared
                .fromString(src: "trait Trait {}").value
        )
        let expansion = asyncTrait(args: tokens, input: source)
        let expansionDiagnostic = try #require(expansion.error)
        let expansionMessage: String = expansionDiagnostic.message
        #expect(expansionMessage.contains("expected"))
        #expect(expansion.value == nil)
    }
}
