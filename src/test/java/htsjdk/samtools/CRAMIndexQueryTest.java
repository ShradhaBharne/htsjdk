/*
 * The MIT License
 *
 * Copyright (c) 2016 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package htsjdk.samtools;

import htsjdk.samtools.util.CloseableIterator;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * Test index query calls against a *SamReader* on a CRAM File, which will use
 * whatever index format (.bai or .crai converted to .bai) is available for the
 * target file.
 */
public class CRAMIndexQueryTest {

    private static final File TEST_DATA_DIR = new File("src/test/resources/htsjdk/samtools/cram");

    private static final String cramQueryWithBAI = "cramQueryWithBAI.cram";
    private static final String cramQueryWithCRAI = "cramQueryWithCRAI.cram";
    private static final String cramQueryReference = "human_g1k_v37.20.21.10M-10M200k.fasta";

    private static final String cramQueryReadsWithBAI = "cramQueryTest.cram";
    private static final String cramQueryTestEmptyWithBAI = "cramQueryTestEmpty.cram";
    private static final String cramQueryReadsReference = "../hg19mini.fasta";

    @DataProvider(name = "singleIntervalOverlapping")
    public Object[][] singleIntervalOverlapping() {
        return new Object[][] {
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 100009, 100009), new String[]{"a", "b", "c"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 100009, 100009), new String[]{"a", "b", "c"}},

            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 100009, 100011), new String[]{"a", "b", "c", "d", "e"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 100009, 100011), new String[]{"a", "b", "c", "d", "e"}},

            // interval with 1 start
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 1, 100010), new String[]{"a", "b", "c", "d"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 1, 100010), new String[]{"a", "b", "c", "d"}},

            // interval with 0 end
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 100015, 0), new String[]{"a", "b", "c", "d", "e", "f"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 100015, 0), new String[]{"a", "b", "c", "d", "e", "f"}},

            // interval with 1 start and 0 end
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 1, 0), new String[]{"a", "b", "c", "d", "e", "f",  "f"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 1, 0), new String[]{"a", "b", "c", "d", "e", "f",  "f"}},

            //totally empty cram file
            {cramQueryTestEmptyWithBAI, cramQueryReadsReference, new QueryInterval(0, 1, 0), new String[]{}},
        };
    }

    @Test(dataProvider="singleIntervalOverlapping")
    public void testQueryOverlappingSingleInterval(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
                reader -> reader.queryOverlapping(new QueryInterval[]{interval}),
                cramFileName,
                referenceFileName,
                expectedNames
        );
    }

    @Test(dataProvider="singleIntervalOverlapping")
    public void testQueryOverlappingSequence(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.queryOverlapping(
                reader.getFileHeader().getSequence(interval.referenceIndex).getSequenceName(),
                interval.start,
                interval.end
            ),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @Test(dataProvider="singleIntervalOverlapping")
    public void testQuerySingleIntervalContainedFalse(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.query(new QueryInterval[]{interval}, false),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @Test(dataProvider="singleIntervalOverlapping")
    public void testQuerySequenceContainedFalse(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.query(
                reader.getFileHeader().getSequence(interval.referenceIndex).getSequenceName(),
                interval.start,
                interval.end,
                false
            ),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @DataProvider(name = "singleIntervalContained")
    public Object[][] singleIntervalContained() {
        return new Object[][] {
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 100013, 100070), new String[]{"f", "f",}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 100013, 100070), new String[]{"f", "f"}},

            // interval with 1 start
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 1, 100100), new String[]{"e", "f", "f"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 1, 100100), new String[]{"e", "f", "f"}},

            // interval with 0 end
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 100010, 0), new String[]{"d", "e", "f", "f",}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 100010, 0), new String[]{"d", "e", "f", "f",}},

            // interval with 1 start and 0 end
            {cramQueryWithCRAI, cramQueryReference, new QueryInterval(0, 1, 0), new String[]{"a", "b", "c", "d", "e", "f",  "f"}},
            {cramQueryWithBAI, cramQueryReference, new QueryInterval(0, 1, 0), new String[]{"a", "b", "c", "d", "e", "f",  "f"}},

            //totally empty cram file
            {cramQueryTestEmptyWithBAI, cramQueryReadsReference, new QueryInterval(0, 1, 0), new String[]{}},
        };
    }

    @Test(dataProvider="singleIntervalContained")
    public void testQueryContainedSingleInterval(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.queryContained(new QueryInterval[]{interval}),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @Test(dataProvider="singleIntervalContained")
    public void testQueryContainedSequence(
        final String cramFileName,
        final String referenceFileName,
        final QueryInterval interval,
        final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.queryContained(
                reader.getFileHeader().getSequence(interval.referenceIndex).getSequenceName(),
                interval.start,
                interval.end
            ),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @Test(dataProvider="singleIntervalContained")
    public void testQuerySingleIntervalContainedTrue(
            final String cramFileName,
            final String referenceFileName,
            final QueryInterval interval,
            final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.query(new QueryInterval[]{interval}, true),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @Test(dataProvider="singleIntervalContained")
    public void testQuerySequenceContainedTrue(
            final String cramFileName,
            final String referenceFileName,
            final QueryInterval interval,
            final String[] expectedNames) throws IOException
    {
        doQueryTest(
            reader -> reader.query(
                reader.getFileHeader().getSequence(interval.referenceIndex).getSequenceName(),
                interval.start,
                interval.end,
                true
            ),
            cramFileName,
            referenceFileName,
            expectedNames
        );
    }

    @DataProvider(name = "multipleIntervalOverlapping")
    public Object[][] multipleIntervalOverlapping() {
        return new Object[][]{
            {cramQueryWithCRAI, cramQueryReference,
                    new QueryInterval[]{new QueryInterval(0, 100010, 100010), new QueryInterval(0, 100011, 100011)},
                    new String[]{"a", "b", "c", "d", "e"}},
            {cramQueryWithBAI, cramQueryReference,
                    new QueryInterval[]{new QueryInterval(0, 100010, 100010), new QueryInterval(0, 100011, 100011)},
                    new String[]{"a", "b", "c", "d", "e"}},
            // no matching reads
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 300, 310), new QueryInterval(1, 300, 310)},
                    new String[]{}},
            // matching reads from first interval only
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 205, 206), new QueryInterval(3, 300, 301)},
                    new String[]{"a", "b"}},
            // matching reads from last interval only
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 100, 101), new QueryInterval(3, 700, 701)},
                    new String[]{"k"}},
            //matching reads from each interval
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 205, 206), new QueryInterval(3, 700, 701)},
                    new String[]{"a", "b", "k"}},
            //matching reads from each interval - 4 intervals
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{
                            new QueryInterval(0, 200, 201), new QueryInterval(1, 500, 501),
                            new QueryInterval(2, 300, 301), new QueryInterval(3, 700, 701)},
                    new String[]{"a", "f", "i", "k"}},
            // first read is before the first interval
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(2, 300, 301), new QueryInterval(3, 700, 701)},
                    new String[]{"i", "k"}},
            // first interval is before the first read
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 100, 101), new QueryInterval(0, 200, 201)},
                    new String[]{"a"}},
            // intervals in reverse order
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 200, 201), new QueryInterval(0, 100, 101)},
                    new String[]{"a"}},
        };
    }

    @Test(dataProvider="multipleIntervalOverlapping")
    public void testQueryOverlappingMultipleIntervals(
            final String cramFileName,
            final String referenceFileName,
            final QueryInterval[] intervals,
            final String[] expectedNames) throws IOException
    {
        QueryInterval[] optimizedIntervals = QueryInterval.optimizeIntervals(intervals);
        Assert.assertTrue(optimizedIntervals.length > 1);

        doQueryTest(
                reader -> reader.queryOverlapping(optimizedIntervals),
                cramFileName,
                referenceFileName,
                expectedNames
        );
    }

    @DataProvider(name = "otherMultipleIntervals")
    public Object[][] otherMultipleIntervals() {
        return new Object[][]{
                // accept an empty QueryIntervalArray
                {cramQueryWithBAI, cramQueryReference,
                        new QueryInterval[]{},
                        new String[]{}},
                // intervals overlapping - optimized to a single interval
                {cramQueryReadsWithBAI, cramQueryReadsReference,
                        new QueryInterval[]{new QueryInterval(0, 1000, 1030), new QueryInterval(0, 1020, 1076)},
                        new String[]{"d"}}
        };
    }

    // these are tested separately because we want the normal multi-interval test to
    // assert that the interval list size is > 1 post-optimization to ensure we're
    // using more than one interval; these tests optimize down to 0 or 1 interval
    @Test(dataProvider="otherMultipleIntervals")
    public void testOtherMultipleIntervals(
            final String cramFileName,
            final String referenceFileName,
            final QueryInterval[] intervals,
            final String[] expectedNames) throws IOException
    {
        QueryInterval[] optimizedIntervals = QueryInterval.optimizeIntervals(intervals);
        doQueryTest(
                reader -> reader.queryContained(optimizedIntervals),
                cramFileName,
                referenceFileName,
                expectedNames
        );
        doQueryTest(
                reader -> reader.queryOverlapping(optimizedIntervals),
                cramFileName,
                referenceFileName,
                expectedNames
        );
    }

    @DataProvider(name = "multipleIntervalContained")
    public Object[][] multipleIntervalContained() {
        return new Object[][]{
            {cramQueryWithCRAI, cramQueryReference,
                    new QueryInterval[]{new QueryInterval(0, 100008, 100008), new QueryInterval(0, 100013, 0)},
                    new String[]{"f", "f"}},
            {cramQueryWithBAI, cramQueryReference,
                    new QueryInterval[]{new QueryInterval(0, 100008, 100008), new QueryInterval(0, 100013, 0)},
                    new String[]{"f", "f"}},
            // no matching reads
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 300, 310), new QueryInterval(1, 300, 310)},
                    new String[]{}},
            // matching reads from first interval only
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 205, 305), new QueryInterval(3, 300, 301)},
                    new String[]{"b", "c"}},
            // matching reads from last interval only
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 100, 101), new QueryInterval(3, 700, 776)},
                    new String[]{"k"}},
            //matching reads from each interval
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 200, 281), new QueryInterval(3, 700, 776)},
                    new String[]{"a", "b", "k"}},
            //matching reads from each interval - 4 intervals
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{
                            new QueryInterval(0, 200, 281), new QueryInterval(1, 500, 576),
                            new QueryInterval(2, 300, 376), new QueryInterval(3, 700, 776)},
                    new String[]{"a", "b", "f", "i", "k"}},
            // first read is before the first interval
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(2, 300, 301), new QueryInterval(3, 700, 776)},
                    new String[]{"k"}},
            // first interval is before the first read
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 100, 101), new QueryInterval(0, 200, 276)},
                    new String[]{"a"}},
            // intervals in reverse order
            {cramQueryReadsWithBAI, cramQueryReadsReference,
                    new QueryInterval[]{new QueryInterval(0, 200, 276), new QueryInterval(0, 100, 101)},
                    new String[]{"a"}},
        };
    }

    @Test(dataProvider="multipleIntervalContained")
    public void testQueryContainedMultipleIntervals(
            final String cramFileName,
            final String referenceFileName,
            final QueryInterval[] intervals,
            final String[] expectedNames) throws IOException
    {
        QueryInterval[] optimizedIntervals = QueryInterval.optimizeIntervals(intervals);
        Assert.assertTrue(optimizedIntervals.length > 1);
        doQueryTest(
                reader -> reader.queryContained(optimizedIntervals),
                cramFileName,
                referenceFileName,
                expectedNames
        );
    }

    @DataProvider(name = "unmappedQueries")
    public Object[][] unmappedQueries() {
        return new Object[][] {
                {cramQueryWithCRAI, cramQueryReference, new String[]{"g", "h", "h", "i", "i"}},
                {cramQueryWithBAI, cramQueryReference, new String[]{"g", "h", "h", "i", "i"}},
                //no unmapped reads
                {cramQueryReadsWithBAI, cramQueryReadsReference, new String[]{}}
        };
    }

    @Test(dataProvider="unmappedQueries")
    public void testQueryUnmapped(
            final String cramFileName,
            final String referenceFileName,
            final String[] expectedNames) throws IOException
    {
        doQueryTest(
                reader -> reader.queryUnmapped(),
                cramFileName,
                referenceFileName,
                expectedNames
        );
    }

    @DataProvider(name = "mateQueries")
    public Object[][] mateQueries() {
        return new Object[][] {
                {cramQueryWithCRAI, cramQueryReference, "f"},
                {cramQueryWithBAI, cramQueryReference, "f"}
        };
    }

    @Test(dataProvider="mateQueries")
    public void testQueryMate(
        final String cramFileName,
        final String referenceFileName,
        final String expectedName) throws IOException
    {
        SamReaderFactory factory = SamReaderFactory.makeDefault();
        if (referenceFileName != null) {
            factory = factory.referenceSequence(new File(TEST_DATA_DIR, referenceFileName));
        }
        SAMRecord firstRecord = null;
        SAMRecord secondRecord = null;
        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName))) {
            final CloseableIterator<SAMRecord> it = reader.queryAlignmentStart("20", 100013);
            Assert.assertTrue(it.hasNext());
            firstRecord = it.next();
            Assert.assertTrue(it.hasNext());
            secondRecord = it.next();
            Assert.assertFalse(it.hasNext());
        }

        // get the mate for the first record
        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName))) {
            final SAMRecord samRecord = reader.queryMate(firstRecord);
            Assert.assertEquals(samRecord, secondRecord);
        }

        // now query the mate's mate to ensure we get symmetric results
        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName))) {
            final SAMRecord samRecord = reader.queryMate(secondRecord);
            Assert.assertEquals(samRecord, firstRecord);
        }
    }

    private void doQueryTest(
        final Function<SamReader, CloseableIterator <SAMRecord>> getIterator,
        final String cramFileName,
        final String referenceFileName,
        final String[] expectedNames) throws IOException
    {
        SamReaderFactory factory = SamReaderFactory.makeDefault();
        if (referenceFileName != null) {
            factory = factory.referenceSequence(new File(TEST_DATA_DIR, referenceFileName));
        }
        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName));
             final CloseableIterator<SAMRecord> it = getIterator.apply(reader)) {
            int count = 0;
            while (it.hasNext()) {
                SAMRecord samRec = it.next();
                Assert.assertTrue(count < expectedNames.length);
                Assert.assertEquals(samRec.getReadName(), expectedNames[count]);
                count++;
            }
            Assert.assertEquals(count, expectedNames.length);
        }
    }


    @DataProvider(name = "iteratorStateTests")
    public Object[][] iteratorStateQueries() {
        return new Object[][] {
                {cramQueryWithCRAI, cramQueryReference},
                {cramQueryWithBAI, cramQueryReference}
        };
    }

    // The current CRAMFileReader implementation allows multiple iterators to exist on a
    // CRAM reader at the same time, but they're not properly isolated from each other. When
    // CRAMFileReader is changed to support the SamReader contract of one-iterator-at-a-time
    // (https://github.com/samtools/htsjdk/issues/563), these can be re-enabled.
    //
    @Test(dataProvider="iteratorStateTests", expectedExceptions=SAMException.class, enabled=false)
    public void testIteratorState(
            final String cramFileName,
            final String referenceFileName,
            final int expectedCount) throws IOException
    {
        SamReaderFactory factory = SamReaderFactory.makeDefault();
        if (referenceFileName != null) {
            factory = factory.referenceSequence(new File(TEST_DATA_DIR, referenceFileName));
        }

        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName))) {
            final CloseableIterator<SAMRecord> origIt = reader.iterator();

            // opening the second iterator should throw
            final CloseableIterator<SAMRecord> overlapIt = reader.queryOverlapping("20", 100013, 100070);
        }
    }

    @DataProvider(name = "unmappedSliceTest")
    public Object[][] unmappedMultiSliceTest() {
        return new Object[][] {
                // the main test feature of these files is that they have several mapped reads followed by
                // some number of unmapped reads, each created with seqs_per_slice = 100 to force the unmapped
                // reads to be distributed over multiple slices (at least for large numbers of unmapped reads)
                // tests the fix to https://github.com/samtools/htsjdk/issues/562
                {"NA12878.20.21.1-100.100-SeqsPerSlice.0-unMapped.cram", "human_g1k_v37.20.21.1-100.fasta", 0},
                {"NA12878.20.21.1-100.100-SeqsPerSlice.1-unMapped.cram", "human_g1k_v37.20.21.1-100.fasta", 1},
                {"NA12878.20.21.1-100.100-SeqsPerSlice.500-unMapped.cram", "human_g1k_v37.20.21.1-100.fasta", 500},
        };
    }

    @Test(dataProvider = "unmappedSliceTest")
    private void testUnmappedMultiSlice(
            final String cramFileName,
            final String referenceFileName,
            final int expectedCount) throws IOException
    {
        SamReaderFactory factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        factory = factory.referenceSequence(new File(TEST_DATA_DIR, referenceFileName));

        int count = 0;
        try (final SamReader reader = factory.open(new File(TEST_DATA_DIR, cramFileName));
             final CloseableIterator<SAMRecord> it = reader.queryUnmapped())
        {
            while (it.hasNext()) {
                it.next();
                count++;
            }
        }
        Assert.assertEquals(count, expectedCount);
    }

}
