import Testing
import AsyncTrait

@Suite("AsyncTrait Swift Export Suite")
struct AsyncTraitExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "AsyncTrait swift module imported cleanly")
    }
}
