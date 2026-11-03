import Testing
import AsyncTrait

@Suite("AsyncTrait Swift Export Suite")
struct AsyncTraitExportTests {
    @Test("Exported arguments preserve their local setting")
    func swiftModuleLoads() {
        let send = Args(local: false)
        let local = Args(local: true)
        #expect(!send.local)
        #expect(local.local)
    }
}
