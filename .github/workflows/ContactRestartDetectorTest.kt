package com.example
import com.example.util.ContactRestartDetector
import org.junit.Assert.assertEquals
import org.junit.Test
class ContactRestartDetectorTest {
    @Test fun recordedSteadyTipButtonSequenceHasNinePressAndNineReleaseCandidates() {
        val samples = listOf(
        ContactRestartDetector.Sample(0, 95488556L, 485.6836f, 995.06836f, 0.01745712f, 12),
        ContactRestartDetector.Sample(1, 95489252L, 485.8484f, 996.6797f, 0.21400231f, 12),
        ContactRestartDetector.Sample(0, 95489264L, 485.8484f, 996.6797f, 0.7459562f, 12),
        ContactRestartDetector.Sample(1, 95489510L, 486.8042f, 993.0176f, 0.7590185f, 12),
        ContactRestartDetector.Sample(0, 95489570L, 486.70532f, 992.7246f, 0.38088262f, 12),
        ContactRestartDetector.Sample(1, 95490884L, 486.34277f, 993.8965f, 0.064701214f, 12),
        ContactRestartDetector.Sample(0, 95490894L, 486.67236f, 993.82324f, 0.62564856f, 12),
        ContactRestartDetector.Sample(1, 95491054L, 486.87012f, 992.28516f, 0.79454315f, 12),
        ContactRestartDetector.Sample(0, 95491114L, 487.56226f, 991.91895f, 0.3819813f, 12),
        ContactRestartDetector.Sample(1, 95494026L, 486.90308f, 991.55273f, 0.32033205f, 12),
        ContactRestartDetector.Sample(0, 95494038L, 486.90308f, 991.55273f, 0.6945004f, 12),
        ContactRestartDetector.Sample(1, 95494304L, 486.5735f, 992.65137f, 0.84520537f, 12),
        ContactRestartDetector.Sample(0, 95494364L, 487.29858f, 992.5781f, 0.40401635f, 12),
        ContactRestartDetector.Sample(1, 95496516L, 487.29858f, 992.5781f, 0.40981504f, 12),
        ContactRestartDetector.Sample(0, 95496568L, 487.1997f, 993.67676f, 0.9135689f, 12),
        ContactRestartDetector.Sample(1, 95496838L, 486.70532f, 992.65137f, 0.8740768f, 12),
        ContactRestartDetector.Sample(0, 95496898L, 487.10083f, 992.5049f, 0.42318258f, 12),
        ContactRestartDetector.Sample(1, 95499110L, 487.10083f, 992.5049f, 0.3338827f, 12),
        ContactRestartDetector.Sample(0, 95499122L, 487.10083f, 992.5049f, 0.6872978f, 12),
        ContactRestartDetector.Sample(1, 95499426L, 487.10083f, 992.5049f, 0.8699872f, 12),
        ContactRestartDetector.Sample(0, 95499486L, 487.10083f, 992.0654f, 0.41329426f, 12),
        ContactRestartDetector.Sample(1, 95501386L, 487.10083f, 992.0654f, 0.33144113f, 12),
        ContactRestartDetector.Sample(0, 95501396L, 487.10083f, 992.0654f, 0.7596289f, 12),
        ContactRestartDetector.Sample(1, 95501842L, 486.67236f, 992.94434f, 0.8767015f, 12),
        ContactRestartDetector.Sample(0, 95501902L, 486.70532f, 992.8711f, 0.42263323f, 12),
        ContactRestartDetector.Sample(1, 95506796L, 489.8694f, 989.13574f, 0.22688152f, 12),
        ContactRestartDetector.Sample(0, 95506848L, 490.3308f, 990.6006f, 0.5516084f, 12),
        ContactRestartDetector.Sample(1, 95507078L, 490.3308f, 990.6006f, 0.5752304f, 12),
        ContactRestartDetector.Sample(0, 95507136L, 490.03418f, 990.6738f, 0.2827321f, 12),
        ContactRestartDetector.Sample(1, 95509986L, 490.69336f, 989.5752f, 0.1572972f, 12),
        ContactRestartDetector.Sample(0, 95509998L, 489.1443f, 989.2822f, 0.47512665f, 12),
        ContactRestartDetector.Sample(1, 95510330L, 490.13306f, 990.7471f, 0.59903556f, 12),
        ContactRestartDetector.Sample(0, 95510390L, 489.63867f, 991.55273f, 0.28315938f, 12),
        ContactRestartDetector.Sample(1, 95512578L, 489.40796f, 990.2344f, 0.16480498f, 12),
        ContactRestartDetector.Sample(0, 95512590L, 489.40796f, 990.2344f, 0.54196423f, 12),
        ContactRestartDetector.Sample(1, 95512878L, 489.40796f, 990.2344f, 0.6109382f, 12),
        ContactRestartDetector.Sample(0, 95512940L, 489.63867f, 990.7471f, 0.299823f, 12),
        ContactRestartDetector.Sample(1, 95514054L, 487.79297f, 988.6963f, 0.009949338f, 12))
        val detector = ContactRestartDetector()
        val edges = samples.map { detector.observe(it, true) }
        assertEquals(9, edges.count { it == ContactRestartDetector.Edge.PRESS_CANDIDATE })
        assertEquals(9, edges.count { it == ContactRestartDetector.Edge.RELEASE_CANDIDATE })
    }
    @Test fun recordedOrdinaryAndUpperHeldTappingDoNotBecomeRestartPresses() {
        val samples = listOf(
        ContactRestartDetector.Sample(0, 94784525L, 551.4038f, 1072.1924f, 0.085454434f, 12),
        ContactRestartDetector.Sample(1, 94784713L, 548.73413f, 1070.0684f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94786397L, 449.5935f, 1230.3223f, 0.111762196f, 12),
        ContactRestartDetector.Sample(1, 94786575L, 449.82422f, 1232.5195f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94789823L, 527.34375f, 1171.1426f, 0.07062198f, 12),
        ContactRestartDetector.Sample(1, 94789965L, 520.8838f, 1171.7285f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94791897L, 802.1228f, 1110.6445f, 0.04040774f, 12),
        ContactRestartDetector.Sample(1, 94792001L, 804.1992f, 1111.8164f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94794193L, 588.5156f, 1361.3525f, 0.046084356f, 12),
        ContactRestartDetector.Sample(1, 94794333L, 589.9658f, 1360.8398f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94796449L, 245.01709f, 850.1953f, 0.029054508f, 12),
        ContactRestartDetector.Sample(1, 94796617L, 243.30322f, 850.1953f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94799307L, 478.16895f, 784.9365f, 0.035707746f, 12),
        ContactRestartDetector.Sample(1, 94799471L, 476.9165f, 785.9619f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94801287L, 315.28564f, 1188.2812f, 0.029298663f, 12),
        ContactRestartDetector.Sample(1, 94801447L, 312.48413f, 1189.8193f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94803159L, 764.0552f, 821.77734f, 0.026246719f, 12),
        ContactRestartDetector.Sample(1, 94803281L, 761.91284f, 823.2422f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94805021L, 254.64111f, 1057.6904f, 0.026368797f, 12),
        ContactRestartDetector.Sample(1, 94805135L, 249.00513f, 1064.209f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94807291L, 447.28638f, 979.3213f, 0.018677898f, 12),
        ContactRestartDetector.Sample(1, 94807371L, 448.67065f, 974.56055f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94811571L, 803.7378f, 1644.5801f, 0.02948178f, 12),
        ContactRestartDetector.Sample(1, 94811717L, 804.00146f, 1641.7969f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94814051L, 434.13574f, 1754.1504f, 0.039431117f, 12),
        ContactRestartDetector.Sample(1, 94814189L, 439.0796f, 1749.9756f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94816605L, 321.81152f, 1555.8838f, 0.029786974f, 12),
        ContactRestartDetector.Sample(1, 94816737L, 325.437f, 1546.0693f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94819025L, 928.55347f, 1367.8711f, 0.04095709f, 12),
        ContactRestartDetector.Sample(1, 94819105L, 930.82764f, 1369.5557f, 0.009949338f, 12),
        ContactRestartDetector.Sample(0, 94867175L, 578.69385f, 1193.6279f, 0.46865654f, 12),
        ContactRestartDetector.Sample(1, 94867376L, 577.96875f, 1191.5039f, 0.0012818165f, 12),
        ContactRestartDetector.Sample(0, 94869246L, 402.49512f, 1219.1895f, 0.15693097f, 12),
        ContactRestartDetector.Sample(1, 94869399L, 401.60522f, 1218.8965f, 0.055850577f, 12),
        ContactRestartDetector.Sample(0, 94871395L, 571.344f, 919.6289f, 0.42751634f, 12),
        ContactRestartDetector.Sample(1, 94871572L, 570.75073f, 920.1416f, 0.025270097f, 12),
        ContactRestartDetector.Sample(0, 94873416L, 300.0586f, 1006.6406f, 0.31264114f, 12),
        ContactRestartDetector.Sample(1, 94873536L, 301.64062f, 1006.5674f, 0.05835317f, 12),
        ContactRestartDetector.Sample(0, 94875748L, 852.48413f, 874.8779f, 0.5679668f, 12),
        ContactRestartDetector.Sample(1, 94875872L, 851.2976f, 873.6328f, 0.09296222f, 12),
        ContactRestartDetector.Sample(0, 94877686L, 797.7063f, 1097.4609f, 0.51266557f, 12),
        ContactRestartDetector.Sample(1, 94877812L, 796.4209f, 1096.582f, 0.05084539f, 12),
        ContactRestartDetector.Sample(0, 94880466L, 447.38525f, 1411.9629f, 0.42379296f, 12),
        ContactRestartDetector.Sample(1, 94880574L, 447.7478f, 1409.9854f, 0.010681804f, 12),
        ContactRestartDetector.Sample(0, 94882604L, 266.5393f, 1213.5498f, 0.3414515f, 12),
        ContactRestartDetector.Sample(1, 94882738L, 266.5393f, 1213.5498f, 0.06659342f, 12),
        ContactRestartDetector.Sample(0, 94884792L, 705.3882f, 929.7363f, 0.5488006f, 12),
        ContactRestartDetector.Sample(1, 94884898L, 705.0586f, 927.3926f, 0.019044131f, 12),
        ContactRestartDetector.Sample(0, 94887454L, 416.30493f, 1075.8545f, 0.2924373f, 12),
        ContactRestartDetector.Sample(1, 94887558L, 416.239f, 1074.9023f, 0.016785692f, 12),
        ContactRestartDetector.Sample(0, 94888934L, 855.28564f, 1394.3848f, 0.49807727f, 12),
        ContactRestartDetector.Sample(1, 94889076L, 854.9231f, 1390.6494f, 0.003051944f, 12),
        ContactRestartDetector.Sample(0, 94890810L, 277.61353f, 775.3418f, 0.31801257f, 12),
        ContactRestartDetector.Sample(1, 94890924L, 278.14087f, 776.001f, 0.0059207715f, 12),
        ContactRestartDetector.Sample(0, 94892726L, 693.7207f, 747.65625f, 0.59226024f, 12),
        ContactRestartDetector.Sample(1, 94892818L, 698.0054f, 742.8955f, 0.022645425f, 12),
        ContactRestartDetector.Sample(0, 94895154L, 410.93262f, 835.18066f, 0.31935543f, 12),
        ContactRestartDetector.Sample(1, 94895252L, 411.5918f, 832.54395f, 0.04370384f, 12),
        ContactRestartDetector.Sample(0, 94897438L, 169.57397f, 1186.377f, 0.28340352f, 12),
        ContactRestartDetector.Sample(1, 94897466L, 168.98071f, 1187.0361f, 0.0032960996f, 12),
        ContactRestartDetector.Sample(0, 94897496L, 168.45337f, 1188.794f, 0.0590246f, 12),
        ContactRestartDetector.Sample(1, 94897520L, 169.3103f, 1188.8672f, 0.0011597387f, 12),
        ContactRestartDetector.Sample(0, 94910452L, 179.69238f, 812.40234f, 0.14997253f, 12),
        ContactRestartDetector.Sample(1, 94910590L, 178.90137f, 812.6221f, 0.016785692f, 12))
        val detector = ContactRestartDetector()
        assertEquals(0, samples.count { detector.observe(it, true) == ContactRestartDetector.Edge.PRESS_CANDIDATE })
    }
    @Test fun aRestartFromAnotherDeviceIsRejected() {
        val detector = ContactRestartDetector()
        detector.observe(ContactRestartDetector.Sample(1, 100L, 10f, 10f, 0.3f, 12), true)
        assertEquals(ContactRestartDetector.Edge.NONE, detector.observe(ContactRestartDetector.Sample(0, 112L, 10f, 10f, 0.7f, 13), true))
    }
    @Test fun heldStateRestoresOnReleaseAndDoesNotToggleOnRepeatedPress() {
        val held = com.example.util.HeldBarrelState()
        held.update(ContactRestartDetector.Edge.PRESS_CANDIDATE)
        held.update(ContactRestartDetector.Edge.PRESS_CANDIDATE)
        assertEquals(true, held.pressed)
        held.update(ContactRestartDetector.Edge.RELEASE_CANDIDATE)
        assertEquals(false, held.pressed)
        held.update(ContactRestartDetector.Edge.PRESS_CANDIDATE)
        held.clear()
        assertEquals(false, held.pressed)
    }
    @Test fun movingPenReleaseIsNotLostAcrossTheRecordedRestart() {
        val detector = ContactRestartDetector()
        detector.observe(ContactRestartDetector.Sample(1, 96662747L, 0f, 0f, 0.20661661f, 12), true)
        assertEquals(ContactRestartDetector.Edge.RELEASE_CANDIDATE, detector.observe(ContactRestartDetector.Sample(0, 96662805L, 16.66724f, 0f, 0.06695965f, 12), true))
    }

    @Test fun preHeldRecordingHasFiveHoverPressCandidates() {
        val detector = ContactRestartDetector()
        val samples = listOf(
            true to ContactRestartDetector.Sample(9, 99241202L, 604.5337f, 721.1426f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99245958L, 678.42773f, 719.458f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99246004L, 681.62476f, 719.458f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99246736L, 685.57983f, 714.47754f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99246736L, 685.57983f, 714.47754f, 0.11005311f, 12),
            false to ContactRestartDetector.Sample(1, 99249492L, 559.9072f, 971.5576f, 0.66416407f, 12),
            true to ContactRestartDetector.Sample(9, 99249528L, 558.5559f, 970.8252f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99249552L, 556.4136f, 970.459f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99249552L, 556.4136f, 970.459f, 0.109076485f, 12),
            false to ContactRestartDetector.Sample(1, 99249574L, 557.30347f, 969.7998f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99249574L, 557.30347f, 969.7998f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99251662L, 416.30493f, 1129.9805f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99251912L, 19.281006f, 1437.4512f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99251922L, 15.292969f, 1439.209f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99252090L, 0.0f, 1468.4326f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99252694L, 75.805664f, 1364.6484f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99252694L, 75.805664f, 1364.6484f, 0.026796069f, 12),
            false to ContactRestartDetector.Sample(1, 99252798L, 74.6521f, 1362.8906f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99252798L, 74.6521f, 1362.8906f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99254548L, 102.30469f, 1692.1875f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99254548L, 102.30469f, 1692.1875f, 0.023561008f, 12),
            false to ContactRestartDetector.Sample(1, 99255550L, 98.74512f, 1685.4492f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99255550L, 98.41553f, 1683.4717f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99255844L, 97.196045f, 1662.5977f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99257198L, 333.57788f, 1063.6963f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99257386L, 449.989f, 903.36914f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99257754L, 703.4436f, 720.6299f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99260802L, 612.3779f, 745.8252f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99260848L, 614.45435f, 745.38574f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99261672L, 617.42065f, 747.80273f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99261672L, 617.42065f, 747.80273f, 0.039248f, 12),
            false to ContactRestartDetector.Sample(1, 99264870L, 450.6482f, 1090.5029f, 0.19526339f, 12),
            true to ContactRestartDetector.Sample(9, 99264914L, 450.54932f, 1089.331f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99264928L, 450.54932f, 1089.331f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99264928L, 450.54932f, 1089.331f, 0.08441678f, 12),
            false to ContactRestartDetector.Sample(1, 99265976L, 449.13208f, 1089.4775f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99265976L, 449.13208f, 1089.4775f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99266914L, 612.9053f, 728.3203f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99267448L, 470.8191f, 713.7451f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99269072L, 511.58936f, 648.85254f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99269116L, 512.1826f, 651.26953f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99269354L, 513.92944f, 654.1992f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99269354L, 513.92944f, 654.1992f, 0.02838308f, 12),
            false to ContactRestartDetector.Sample(1, 99272008L, 348.1128f, 1063.3301f, 0.1877556f, 12),
            true to ContactRestartDetector.Sample(9, 99272052L, 347.91504f, 1064.8682f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99272068L, 348.44238f, 1063.6963f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99272068L, 348.44238f, 1063.6963f, 0.090032354f, 12),
            false to ContactRestartDetector.Sample(1, 99272716L, 346.39893f, 1062.0117f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99272716L, 346.39893f, 1062.0117f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99274866L, 464.98535f, 630.10254f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99274866L, 464.98535f, 630.10254f, 0.046267472f, 12),
            false to ContactRestartDetector.Sample(1, 99276872L, 318.7793f, 1046.6309f, 0.56930965f, 12),
            true to ContactRestartDetector.Sample(9, 99276916L, 320.09766f, 1045.5322f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99276932L, 320.09766f, 1045.5322f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99276932L, 320.09766f, 1045.5322f, 0.279436f, 12),
            false to ContactRestartDetector.Sample(1, 99277332L, 320.88867f, 1036.084f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99277332L, 320.88867f, 1036.084f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99279036L, 406.48315f, 621.8262f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99279080L, 407.04346f, 620.6543f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99279673L, 410.93262f, 620.94727f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99279673L, 410.93262f, 620.94727f, 0.115668684f, 12),
            false to ContactRestartDetector.Sample(1, 99281288L, 261.13403f, 1029.0527f, 0.60056156f, 12),
            true to ContactRestartDetector.Sample(9, 99281334L, 261.62842f, 1027.7344f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99281348L, 261.62842f, 1027.7344f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99281348L, 261.62842f, 1027.7344f, 0.32106453f, 12),
            false to ContactRestartDetector.Sample(1, 99281886L, 262.0569f, 1021.94824f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99281886L, 262.0569f, 1021.94824f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99282942L, 366.07544f, 591.72363f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 99282988L, 365.97656f, 595.89844f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99283452L, 365.74585f, 600.6592f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99283452L, 365.74585f, 600.6592f, 0.048770066f, 12),
            false to ContactRestartDetector.Sample(1, 99285390L, 230.84473f, 1020.6299f, 0.3089178f, 12),
            true to ContactRestartDetector.Sample(9, 99285428L, 232.13013f, 1009.79004f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99285450L, 232.13013f, 1009.79004f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99285450L, 232.13013f, 1009.79004f, 0.128731f, 12),
            false to ContactRestartDetector.Sample(1, 99285866L, 232.26196f, 1006.34766f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99285866L, 231.40503f, 1005.32227f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99287748L, 326.06323f, 586.74316f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99287748L, 326.06323f, 586.74316f, 0.02349997f, 12),
            false to ContactRestartDetector.Sample(1, 99289438L, 203.15918f, 955.0781f, 0.3120918f, 12),
            true to ContactRestartDetector.Sample(9, 99289476L, 202.89551f, 951.85547f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99289500L, 202.63184f, 953.2471f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99289500L, 202.63184f, 953.2471f, 0.13178295f, 12),
            false to ContactRestartDetector.Sample(1, 99289816L, 204.21387f, 955.44434f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99289816L, 204.21387f, 955.44434f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99291696L, 569.69604f, 715.20996f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99291696L, 569.69604f, 715.20996f, 0.007141549f, 12),
            false to ContactRestartDetector.Sample(1, 99293372L, 426.2915f, 1067.8711f, 0.2607581f, 12),
            true to ContactRestartDetector.Sample(9, 99293418L, 429.0271f, 1063.3301f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99293434L, 429.06006f, 1061.499f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 99293434L, 429.06006f, 1061.499f, 0.14405176f, 12),
            false to ContactRestartDetector.Sample(1, 99293712L, 426.39038f, 1055.6396f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 99293712L, 426.39038f, 1055.6396f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 99293846L, 418.71094f, 1012.06055f, 0.0f, 12))
        var candidates = 0
        samples.forEach { (hover, sample) ->
            val edge = if(hover) detector.observeHover(sample, true) else detector.observe(sample, true)
            if(hover && edge == ContactRestartDetector.Edge.PRESS_CANDIDATE) candidates++
        }
        assertEquals(5, candidates)
    }

    @Test fun normalTapRecordingDoesNotMatchHoverSignature() {
        val detector = ContactRestartDetector()
        val samples = listOf(
            true to ContactRestartDetector.Sample(9, 94783229L, 593.95386f, 1075.0488f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94784525L, 551.4038f, 1072.1924f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94784525L, 551.4038f, 1072.1924f, 0.085454434f, 12),
            false to ContactRestartDetector.Sample(1, 94784713L, 548.73413f, 1070.0684f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94784713L, 548.73413f, 1070.0684f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94784763L, 545.73486f, 1067.0654f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94785750L, 464.2273f, 1231.6406f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94786397L, 449.5935f, 1230.3223f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94786397L, 449.5935f, 1230.3223f, 0.111762196f, 12),
            false to ContactRestartDetector.Sample(1, 94786575L, 449.82422f, 1232.5195f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94786575L, 449.82422f, 1232.5195f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94786627L, 463.43628f, 1214.502f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94788523L, 797.5415f, 1435.2539f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94789283L, 650.6763f, 1191.7969f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94789571L, 521.31226f, 1156.5674f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94789823L, 527.34375f, 1171.1426f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94789823L, 527.34375f, 1171.1426f, 0.07062198f, 12),
            false to ContactRestartDetector.Sample(1, 94789965L, 520.8838f, 1171.7285f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94789965L, 520.8838f, 1171.7285f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94790081L, 513.4021f, 1172.0215f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94791689L, 789.63135f, 1088.7451f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94791897L, 802.1228f, 1110.6445f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94791897L, 802.1228f, 1110.6445f, 0.04040774f, 12),
            false to ContactRestartDetector.Sample(1, 94792001L, 804.1992f, 1111.8164f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94792001L, 804.1992f, 1111.8164f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94792133L, 799.84863f, 1092.9932f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94793653L, 544.845f, 1285.0342f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94794193L, 588.5156f, 1361.3525f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94794193L, 588.5156f, 1361.3525f, 0.046084356f, 12),
            false to ContactRestartDetector.Sample(1, 94794333L, 589.9658f, 1360.8398f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94794333L, 591.1194f, 1359.5215f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94794407L, 590.59204f, 1335.791f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94795919L, 202.86255f, 815.1123f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94796449L, 245.01709f, 850.1953f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94796449L, 245.01709f, 850.1953f, 0.029054508f, 12),
            false to ContactRestartDetector.Sample(1, 94796617L, 243.30322f, 850.1953f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94796617L, 243.30322f, 850.1953f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94796683L, 242.18262f, 834.0088f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94797780L, 487.62817f, 741.21094f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94799307L, 478.16895f, 784.9365f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94799307L, 478.16895f, 784.9365f, 0.035707746f, 12),
            false to ContactRestartDetector.Sample(1, 94799471L, 476.9165f, 785.9619f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94799471L, 476.9165f, 785.9619f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94799527L, 474.70825f, 771.6797f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94800723L, 305.5298f, 1196.5576f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94801287L, 315.28564f, 1188.2812f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94801287L, 315.28564f, 1188.2812f, 0.029298663f, 12),
            false to ContactRestartDetector.Sample(1, 94801447L, 312.48413f, 1189.8193f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94801447L, 311.6272f, 1190.6982f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94801499L, 306.8152f, 1166.0889f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94802583L, 749.12476f, 775.4883f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94803159L, 764.0552f, 821.77734f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94803159L, 764.0552f, 821.77734f, 0.026246719f, 12),
            false to ContactRestartDetector.Sample(1, 94803281L, 761.91284f, 823.2422f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94803281L, 760.6604f, 823.02246f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94803323L, 748.3008f, 809.98535f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94804626L, 243.89648f, 1048.5352f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94805021L, 254.64111f, 1057.6904f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94805021L, 254.64111f, 1057.6904f, 0.026368797f, 12),
            false to ContactRestartDetector.Sample(1, 94805135L, 249.00513f, 1064.209f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94805135L, 249.00513f, 1064.209f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94805189L, 238.2605f, 1049.6338f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94806614L, 489.0454f, 1012.06055f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94807291L, 447.28638f, 979.3213f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94807291L, 447.28638f, 979.3213f, 0.018677898f, 12),
            false to ContactRestartDetector.Sample(1, 94807371L, 448.67065f, 974.56055f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94807371L, 448.67065f, 974.56055f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94807417L, 445.7373f, 951.3428f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94811017L, 807.9895f, 1699.292f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94811571L, 803.7378f, 1644.5801f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94811571L, 803.7378f, 1644.5801f, 0.02948178f, 12),
            false to ContactRestartDetector.Sample(1, 94811717L, 804.00146f, 1641.7969f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94811717L, 804.00146f, 1641.7969f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94811765L, 798.0029f, 1638.501f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94813541L, 406.02173f, 1776.3428f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94814051L, 434.13574f, 1754.1504f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94814051L, 434.13574f, 1754.1504f, 0.039431117f, 12),
            false to ContactRestartDetector.Sample(1, 94814189L, 439.0796f, 1749.9756f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94814189L, 439.0796f, 1749.9756f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94814255L, 427.11548f, 1748.9502f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94816125L, 298.80615f, 1559.8389f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94816605L, 321.54785f, 1557.2754f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94816605L, 321.81152f, 1555.8838f, 0.029786974f, 12),
            false to ContactRestartDetector.Sample(1, 94816737L, 325.437f, 1546.0693f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94816737L, 325.437f, 1546.0693f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94816805L, 317.29614f, 1535.8154f, 0.0f, 12),
            true to ContactRestartDetector.Sample(9, 94818413L, 972.8174f, 1380.3223f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94819025L, 929.4104f, 1367.8711f, 0.0f, 12),
            false to ContactRestartDetector.Sample(0, 94819025L, 928.55347f, 1367.8711f, 0.04095709f, 12),
            false to ContactRestartDetector.Sample(1, 94819105L, 930.82764f, 1369.5557f, 0.009949338f, 12),
            true to ContactRestartDetector.Sample(9, 94819105L, 932.0471f, 1368.8232f, 0.0f, 12),
            true to ContactRestartDetector.Sample(10, 94819171L, 932.8052f, 1360.1807f, 0.0f, 12))
        var candidates = 0
        samples.forEach { (hover, sample) ->
            val edge = if(hover) detector.observeHover(sample, true) else detector.observe(sample, true)
            if(hover && edge == ContactRestartDetector.Edge.PRESS_CANDIDATE) candidates++
        }
        assertEquals(0, candidates)
    }

    @Test fun recordedMovingHoverPressesAndReleasesAreSeparated() {
        listOf(12.2f, 13.63f).forEach { travel ->
            val detector = ContactRestartDetector()
            detector.observeHover(ContactRestartDetector.Sample(10, 100L, 0f, 0f, 0f, 12), true)
            assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE, detector.observeHover(ContactRestartDetector.Sample(9, 146L, travel, 0f, 0f, 12), true))
        }
        val detector = ContactRestartDetector()
        detector.observeHover(ContactRestartDetector.Sample(10, 200L, 0f, 0f, 0f, 12), true)
        assertEquals(ContactRestartDetector.Edge.RELEASE_CANDIDATE, detector.observeHover(ContactRestartDetector.Sample(9, 238L, 56.61f, 0f, 0f, 12), true))
    }
    @Test fun aTipLiftDoesNotReleaseTheHeldToolState() {
        val held = com.example.util.HeldBarrelState()
        held.update(ContactRestartDetector.Edge.PRESS_CANDIDATE)
        val detector = ContactRestartDetector()
        held.update(detector.observe(ContactRestartDetector.Sample(1, 100L, 0f, 0f, 0.009949338f, 12), true))
        assertEquals(true, held.pressed)
        held.update(ContactRestartDetector.Edge.RELEASE_CANDIDATE)
        assertEquals(false, held.pressed)
    }

}
