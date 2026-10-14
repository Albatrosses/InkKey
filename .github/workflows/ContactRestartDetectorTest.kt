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

}
