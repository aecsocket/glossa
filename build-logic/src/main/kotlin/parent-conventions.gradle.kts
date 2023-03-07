plugins {
    kotlin("jvm") apply false
    id("net.kyori.indra.publishing.sonatype")
}

indraSonatype {
    useAlternateSonatypeOSSHost("s01")
}
