package com.example.riwaz.ml

/**
 * Culturally-aware Raga Sequence Database
 *
 * Provides expert-curated melodic sequence patterns for each raga:
 *   - Pakad: Characteristic catch-phrases that uniquely identify the raga
 *   - Chalan: Characteristic movement idioms (how the raga "walks")
 *   - Aroha: Standard ascending scale phrases
 *   - Avaroha: Standard descending scale phrases
 *   - Vadi/Samvadi emphasis windows: sequences that anchor the dominant notes
 *
 * Patterns are encoded as integer pitch-class sequences (0 = Sa, 1 = Re(k)/Re, etc.)
 * following standard 12-TET semitone numbering. Uses octave tags (+12) for upper
 * octave notes.
 *
 * Sources: Expert knowledge aligned with CompMusic corpus conventions and
 * traditional treatises (Sangeet Ratnakar, Bhatkhande notation).
 */
object RagaSequenceDatabase {

    /**
     * Represents a single labeled melodic sequence pattern.
     * @param label      Human-readable name for this pattern
     * @param notes      Pitch-class integer sequence (semitones from tonic, octave-aware)
     * @param type       What kind of pattern this is
     * @param weight     Importance weight for scoring (higher = more distinctive)
     * @param isOptional Whether failing to find this pattern should penalise the score
     */
    data class MelodicPattern(
        val label: String,
        val notes: List<Int>,
        val type: PatternType,
        val weight: Float = 1.0f,
        val isOptional: Boolean = false
    )

    enum class PatternType {
        PAKAD,    // Catch-phrase / signature phrase of the raga
        CHALAN,   // Characteristic melodic movement idiom
        AROHA,    // Ascending scale movement
        AVAROHA,  // Descending scale movement
        VADI_EMPHASIS,    // Phrase that centres on the vadi (dominant note)
        SAMVADI_EMPHASIS  // Phrase that centres on the samvadi
    }

    /**
     * Full sequence profile for a raga.
     */
    data class RagaSequenceProfile(
        val ragaName: String,
        val vadiSwar: Int,       // Dominant swar (pitch class)
        val samvadiSwar: Int,    // Sub-dominant swar (pitch class)
        val allowedNotes: Set<Int>,
        val patterns: List<MelodicPattern>,
        val forbiddenBigrams: List<Pair<Int, Int>>,
        val forbiddenTrigrams: List<Triple<Int, Int, Int>> = emptyList()
    )

    // -----------------------------------------------------------------------
    // Pitch-class constants for readability
    // -----------------------------------------------------------------------
    private const val SA = 0
    private const val RE_K = 1   // Komal Re
    private const val RE = 2     // Shuddha Re
    private const val GA_K = 3   // Komal Ga
    private const val GA = 4     // Shuddha Ga
    private const val MA = 5     // Shuddha Ma
    private const val MA_T = 6   // Tivra Ma
    private const val PA = 7
    private const val DHA_K = 8  // Komal Dha
    private const val DHA = 9    // Shuddha Dha
    private const val NI_K = 10  // Komal Ni
    private const val NI = 11    // Shuddha Ni
    // Upper octave (add 12)
    private const val SA2 = 12
    private const val RE2 = 14
    private const val GA2 = 16
    private const val MA2 = 17
    private const val MA2_T = 18
    private const val PA2 = 19
    private const val DHA2_K = 20
    private const val DHA2 = 21
    private const val NI2 = 23

    // -----------------------------------------------------------------------
    // Raga profiles map
    // -----------------------------------------------------------------------
    private val profiles: Map<String, RagaSequenceProfile> by lazy { buildProfiles() }

    fun getProfile(ragaName: String): RagaSequenceProfile? = profiles[ragaName]
    fun getAllRagaNames(): Set<String> = profiles.keys

    // -----------------------------------------------------------------------
    // Profile construction
    // -----------------------------------------------------------------------
    private fun buildProfiles(): Map<String, RagaSequenceProfile> {
        return mapOf(
            "Yaman" to yamanProfile(),
            "Bhairav" to bhairavProfile(),
            "Todi" to todiProfile(),
            "Malkauns" to malkaunsProfile(),
            "Bhupali" to bhupaliProfile(),
            "Desh" to deshProfile(),
            "Kafi" to kafiProfile(),
            "Bihag" to bihagProfile(),
            "Bageshree" to bageshreeProfile(),
            "Puriya Dhanashree" to puriyaDhanashreeProfile()
        )
    }

    // -------------------------------------------------------------------
    // YAMAN — evening raga, all shuddha except Tivra Ma
    // -------------------------------------------------------------------
    private fun yamanProfile() = RagaSequenceProfile(
        ragaName = "Yaman",
        vadiSwar = GA,
        samvadiSwar = NI,
        allowedNotes = setOf(SA, RE, GA, MA_T, PA, DHA, NI),
        patterns = listOf(
            MelodicPattern(
                "Yaman Pakad 1", listOf(NI, RE, GA, MA_T, GA, RE),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Yaman Pakad 2", listOf(NI, SA, RE, GA, MA_T, PA),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Yaman Aroha", listOf(SA, RE, GA, MA_T, PA, DHA, NI, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Yaman Avaroha", listOf(SA2, NI, DHA, PA, MA_T, GA, RE, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Yaman Chalan 1", listOf(NI, RE, GA, MA_T, PA, DHA, NI, SA2, NI, DHA, PA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Yaman Chalan 2", listOf(GA, MA_T, PA, MA_T, GA, RE, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Yaman Vadi (Ga)", listOf(RE, GA, MA_T, GA, PA, GA),
                PatternType.VADI_EMPHASIS, weight = 1.2f
            ),
            MelodicPattern(
                "Yaman Samvadi (Ni)", listOf(DHA, NI, SA2, NI, DHA, PA),
                PatternType.SAMVADI_EMPHASIS, weight = 1.2f
            ),
            MelodicPattern(
                "Tivra Ma emphasis", listOf(SA, RE, GA, MA_T, MA_T, PA),
                PatternType.CHALAN, weight = 1.8f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(MA_T, RE),   // Skip Ma(t)→Re directly
            Pair(GA, SA)      // Avoid Ga→Sa
        ),
        forbiddenTrigrams = listOf(
            Triple(MA, GA, RE)  // Shuddha Ma never used
        )
    )

    // -------------------------------------------------------------------
    // BHAIRAV — morning raga, komal Re and Dha, rest shuddha
    // -------------------------------------------------------------------
    private fun bhairavProfile() = RagaSequenceProfile(
        ragaName = "Bhairav",
        vadiSwar = DHA_K,
        samvadiSwar = RE_K,
        allowedNotes = setOf(SA, RE_K, GA, MA, PA, DHA_K, NI),
        patterns = listOf(
            MelodicPattern(
                "Bhairav Pakad 1", listOf(SA, RE_K, SA, GA, MA, PA),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Bhairav Pakad 2", listOf(PA, DHA_K, PA, MA, GA, RE_K, SA),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Bhairav Aroha", listOf(SA, RE_K, GA, MA, PA, DHA_K, NI, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bhairav Avaroha", listOf(SA2, NI, DHA_K, PA, MA, GA, RE_K, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bhairav Chalan 1", listOf(SA, RE_K, SA, GA, MA, PA, DHA_K, PA, MA, GA, RE_K, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Bhairav Komal Re Gamak", listOf(SA, RE_K, SA, RE_K, GA),
                PatternType.CHALAN, weight = 1.8f
            ),
            MelodicPattern(
                "Bhairav Vadi (Dha komal)", listOf(PA, DHA_K, NI, SA2, DHA_K, PA),
                PatternType.VADI_EMPHASIS, weight = 1.2f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(RE_K, GA_K), // No komal Ga in Bhairav
            Pair(DHA_K, NI2), // Ni→upper must go via Sa
            Pair(GA, PA)      // Avoid direct Ga→Pa jump
        )
    )

    // -------------------------------------------------------------------
    // TODI — late morning raga, komal Re Ga Dha, tivra Ma
    // -------------------------------------------------------------------
    private fun todiProfile() = RagaSequenceProfile(
        ragaName = "Todi",
        vadiSwar = DHA_K,
        samvadiSwar = GA_K,
        allowedNotes = setOf(SA, RE_K, GA_K, MA_T, PA, DHA_K, NI),
        patterns = listOf(
            MelodicPattern(
                "Todi Pakad 1", listOf(GA_K, MA_T, DHA_K, PA),
                PatternType.PAKAD, weight = 2.2f
            ),
            MelodicPattern(
                "Todi Pakad 2", listOf(SA, RE_K, GA_K, MA_T, PA, DHA_K, NI, SA2),
                PatternType.PAKAD, weight = 1.8f
            ),
            MelodicPattern(
                "Todi Avaroha", listOf(SA2, NI, DHA_K, PA, MA_T, GA_K, RE_K, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Todi Chalan – Re(k) oscillation", listOf(SA, RE_K, SA, RE_K, GA_K),
                PatternType.CHALAN, weight = 1.8f
            ),
            MelodicPattern(
                "Todi Chalan – Upper", listOf(PA, DHA_K, NI, SA2, NI, DHA_K, PA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Todi Vadi (Dha komal)", listOf(MA_T, DHA_K, PA, DHA_K),
                PatternType.VADI_EMPHASIS, weight = 1.4f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(GA_K, DHA_K), // Direct jump G(k)→Dh(k) is weakening
            Pair(MA_T, RE_K)   // Back-slid after Tivra Ma
        ),
        forbiddenTrigrams = listOf(
            Triple(GA_K, MA, DHA_K) // Shuddha Ma never occurs
        )
    )

    // -------------------------------------------------------------------
    // MALKAUNS — deep night raga, pentatonic, all komal except Ma
    // -------------------------------------------------------------------
    private fun malkaunsProfile() = RagaSequenceProfile(
        ragaName = "Malkauns",
        vadiSwar = MA,
        samvadiSwar = SA,
        allowedNotes = setOf(SA, GA_K, MA, DHA_K, NI_K),
        patterns = listOf(
            MelodicPattern(
                "Malkauns Pakad", listOf(NI_K, DHA_K, MA, GA_K, SA),
                PatternType.PAKAD, weight = 2.5f
            ),
            MelodicPattern(
                "Malkauns Aroha", listOf(SA, GA_K, MA, DHA_K, NI_K, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Malkauns Avaroha", listOf(SA2, NI_K, DHA_K, MA, GA_K, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Malkauns Chalan 1", listOf(SA, GA_K, MA, DHA_K, MA, GA_K, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Malkauns Chalan 2", listOf(DHA_K, NI_K, SA2, NI_K, DHA_K, MA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Malkauns Vadi (Ma)", listOf(GA_K, MA, DHA_K, MA, GA_K),
                PatternType.VADI_EMPHASIS, weight = 1.4f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(NI_K, GA_K), // Avoid tritone jump
            Pair(DHA_K, SA)   // Direct Dha(k)→Sa is weak in Malkauns
        )
    )

    // -------------------------------------------------------------------
    // BHUPALI — evening raga, pentatonic Sa Re Ga Pa Dha
    // -------------------------------------------------------------------
    private fun bhupaliProfile() = RagaSequenceProfile(
        ragaName = "Bhupali",
        vadiSwar = GA,
        samvadiSwar = DHA,
        allowedNotes = setOf(SA, RE, GA, PA, DHA),
        patterns = listOf(
            MelodicPattern(
                "Bhupali Pakad", listOf(GA, RE, SA, DHA + (-12), RE, SA),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Bhupali Aroha", listOf(SA, RE, GA, PA, DHA, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bhupali Avaroha", listOf(SA2, DHA, PA, GA, RE, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bhupali Chalan 1", listOf(SA, RE, GA, PA, GA, RE, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Bhupali Chalan 2", listOf(PA, DHA, SA2, DHA, PA, GA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Bhupali Vadi (Ga)", listOf(RE, GA, PA, GA, RE),
                PatternType.VADI_EMPHASIS, weight = 1.3f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(GA, RE),  // Vakra in avaroha only if preceded by Pa
            Pair(DHA, PA)  // Avoid Dha→Pa without Sa context
        )
    )

    // -------------------------------------------------------------------
    // DESH — night raga, Ni in aroha only, both Ni in avaroha
    // -------------------------------------------------------------------
    private fun deshProfile() = RagaSequenceProfile(
        ragaName = "Desh",
        vadiSwar = PA,
        samvadiSwar = RE,
        allowedNotes = setOf(SA, RE, GA, MA, PA, DHA, NI_K, NI),
        patterns = listOf(
            MelodicPattern(
                "Desh Pakad", listOf(SA, RE, MA, PA, RE),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Desh Avaroha with both Ni", listOf(SA2, NI, NI_K, DHA, PA, MA, GA, RE, SA),
                PatternType.AVAROHA, weight = 1.5f
            ),
            MelodicPattern(
                "Desh Aroha (skip Ga)", listOf(SA, RE, MA, PA, DHA, NI, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Desh Chalan", listOf(SA, RE, MA, PA, RE, MA, PA, DHA, PA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Desh Vadi (Pa)", listOf(MA, PA, DHA, PA, MA, RE),
                PatternType.VADI_EMPHASIS, weight = 1.3f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(GA, PA),    // Ga skipped in aroha
            Pair(NI, GA)     // Ni→Ga is forbidden
        )
    )

    // -------------------------------------------------------------------
    // KAFI — all-time raga, komal Ga and Ni
    // -------------------------------------------------------------------
    private fun kafiProfile() = RagaSequenceProfile(
        ragaName = "Kafi",
        vadiSwar = PA,
        samvadiSwar = RE,
        allowedNotes = setOf(SA, RE, GA_K, MA, PA, DHA, NI_K),
        patterns = listOf(
            MelodicPattern(
                "Kafi Pakad", listOf(SA, GA_K, RE, SA),
                PatternType.PAKAD, weight = 2.0f
            ),
            MelodicPattern(
                "Kafi Aroha", listOf(SA, RE, GA_K, MA, PA, DHA, NI_K, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Kafi Avaroha", listOf(SA2, NI_K, DHA, PA, MA, GA_K, RE, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Kafi Chalan", listOf(SA, GA_K, MA, PA, MA, GA_K, RE, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Kafi Vadi (Pa)", listOf(MA, PA, DHA, NI_K, DHA, PA),
                PatternType.VADI_EMPHASIS, weight = 1.3f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(GA_K, DHA),  // Skip from komal Ga to Dha
            Pair(NI_K, MA)    // Ni(k) → Ma jump
        )
    )

    // -------------------------------------------------------------------
    // BIHAG — night raga, both Ma used, vakra in aroha
    // -------------------------------------------------------------------
    private fun bihagProfile() = RagaSequenceProfile(
        ragaName = "Bihag",
        vadiSwar = GA,
        samvadiSwar = NI,
        allowedNotes = setOf(SA, GA, MA, MA_T, PA, DHA, NI),
        patterns = listOf(
            MelodicPattern(
                "Bihag Pakad", listOf(GA, MA_T, PA, GA, MA, PA),
                PatternType.PAKAD, weight = 2.5f
            ),
            MelodicPattern(
                "Bihag Aroha (vakra)", listOf(SA, GA, MA_T, PA, NI, SA2),
                PatternType.AROHA, weight = 1.2f
            ),
            MelodicPattern(
                "Bihag Avaroha", listOf(SA2, NI, DHA, PA, MA, GA, RE, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bihag Chalan – dual Ma", listOf(GA, MA_T, PA, MA, GA),
                PatternType.CHALAN, weight = 2.0f
            ),
            MelodicPattern(
                "Bihag Vadi (Ga)", listOf(RE, GA, MA_T, PA, GA),
                PatternType.VADI_EMPHASIS, weight = 1.3f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(RE, GA_K),  // Komal Ga absent
            Pair(SA, RE)     // Sa→Re avoided in opening; begin with Ga
        )
    )

    // -------------------------------------------------------------------
    // BAGESHREE — night raga, pentatonic like Malkauns but with Re
    // -------------------------------------------------------------------
    private fun bageshreeProfile() = RagaSequenceProfile(
        ragaName = "Bageshree",
        vadiSwar = MA,
        samvadiSwar = SA,
        allowedNotes = setOf(SA, RE, GA_K, MA, DHA_K, NI_K),
        patterns = listOf(
            MelodicPattern(
                "Bageshree Pakad", listOf(SA, RE, GA_K, MA, GA_K, RE, SA),
                PatternType.PAKAD, weight = 2.3f
            ),
            MelodicPattern(
                "Bageshree Aroha", listOf(SA, GA_K, MA, DHA_K, NI_K, SA2),
                PatternType.AROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bageshree Avaroha", listOf(SA2, NI_K, DHA_K, MA, GA_K, RE, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Bageshree Chalan", listOf(SA, RE, GA_K, MA, DHA_K, MA, GA_K, RE, SA),
                PatternType.CHALAN, weight = 1.5f
            ),
            MelodicPattern(
                "Bageshree Vadi (Ma)", listOf(GA_K, MA, DHA_K, MA, GA_K),
                PatternType.VADI_EMPHASIS, weight = 1.4f
            )
        ),
        forbiddenBigrams = listOf(
            Pair(GA_K, NI_K), // Avoid tritone jump
            Pair(DHA_K, RE)   // Avoid Dha(k) → Re
        )
    )

    // -------------------------------------------------------------------
    // PURIYA DHANASHREE — evening raga, komal Re, tivra Ma, no Pa in aroha
    // -------------------------------------------------------------------
    private fun puriyaDhanashreeProfile() = RagaSequenceProfile(
        ragaName = "Puriya Dhanashree",
        vadiSwar = PA,
        samvadiSwar = RE_K,
        allowedNotes = setOf(SA, RE_K, GA, MA_T, PA, DHA, NI),
        patterns = listOf(
            MelodicPattern(
                "Puriya Dhanashree Pakad", listOf(NI, RE_K, GA, MA_T, GA, RE_K),
                PatternType.PAKAD, weight = 2.5f
            ),
            MelodicPattern(
                "Puriya Dhanashree Aroha (no Pa)", listOf(SA, RE_K, GA, MA_T, DHA, NI, SA2),
                PatternType.AROHA, weight = 1.5f
            ),
            MelodicPattern(
                "Puriya Dhanashree Avaroha", listOf(SA2, NI, DHA, PA, MA_T, GA, RE_K, SA),
                PatternType.AVAROHA, weight = 1.0f
            ),
            MelodicPattern(
                "Puriya Dhanashree Chalan", listOf(NI, SA, RE_K, GA, MA_T, GA, RE_K, NI + (-12)),
                PatternType.CHALAN, weight = 1.8f
            ),
            MelodicPattern(
                "Puriya Dhanashree Vadi (Pa, in avaroha only)", listOf(MA_T, PA, DHA, NI, SA2),
                PatternType.VADI_EMPHASIS, weight = 1.2f, isOptional = true
            )
        ),
        forbiddenBigrams = listOf(
            Pair(RE_K, PA),  // Re(k) → Pa skips tivra Ma
            Pair(SA, PA),    // Sa → Pa directly (Pa avoided in aroha)
            Pair(GA, RE)     // Shuddha Re NOT used
        )
    )
}
