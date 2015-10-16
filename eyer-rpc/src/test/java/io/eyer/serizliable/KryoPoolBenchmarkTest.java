package io.eyer.serizliable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class KryoPoolBenchmarkTest {

	private static final int WARMUP_ITERATIONS = 10000;

	/** Number of runs. */
	private static final int RUN_CNT = 10;

	/** Number of iterations. Set it to something rather big for obtaining meaningful results */
// private static final int ITER_CNT = 200000;
	private static final int ITER_CNT = 10000;
	private static final int SLEEP_BETWEEN_RUNS = 100;

	// not private to prevent the synthetic accessor method
	static KryoFactory factory = new KryoFactory() {
		@Override
		public Kryo create () {
			Kryo kryo = new Kryo();
			kryo.register(SampleObject.class);
			return kryo;
		}
	};

	@Test
	public void testWithoutPool() throws Exception {
		// Warm-up phase: Perform 100000 iterations
		runWithoutPool(1, WARMUP_ITERATIONS, false);
		runWithoutPool(RUN_CNT, ITER_CNT, true);
	}

	@Test
	public void testWithPool() throws Exception {
		KryoPool.Builder builder = new KryoPool.Builder(factory);
		// Warm-up phase: Perform 100000 iterations
		runWithPool(builder, 1, WARMUP_ITERATIONS, false);
		runWithPool(builder, RUN_CNT, ITER_CNT, true);
	}

	@Test
	public void testWithPoolWithSoftReferences() throws Exception {
		KryoPool.Builder builder = new KryoPool.Builder(factory).softReferences();
		// Warm-up phase: Perform 100000 iterations
		runWithPool(builder, 1, WARMUP_ITERATIONS, false);
		runWithPool(builder, RUN_CNT, ITER_CNT, true);
	}

	private void run (String description, Runnable runnable, final int runCount, final int iterCount, boolean outputResults) throws Exception {
		long avgDur = 0;
		long bestTime = Long.MAX_VALUE;

		for (int i = 0; i < runCount; i++) {
			long start = System.nanoTime();

			for (int j = 0; j < iterCount; j++) {
				runnable.run();
			}

			long dur = System.nanoTime() - start;
			dur = TimeUnit.NANOSECONDS.toMillis(dur);

			if (outputResults) System.out.format(">>> %s (run %d): %,d ms\n", description, i + 1, dur);
			avgDur += dur;
			bestTime = Math.min(bestTime, dur);
			systemCleanupAfterRun();
		}

		avgDur /= runCount;

		if (outputResults) {
			System.out.format("\n>>> %s (average): %,d ms", description, avgDur);
			System.out.format("\n>>> %s (best time): %,d ms\n\n", description, bestTime);
		}

	}

	private void runWithoutPool (final int runCount, final int iterCount, boolean outputResults) throws Exception {
		run("Without pool", new Runnable() {
			@Override
			public void run () {
				factory.create();
			}
		}, runCount, iterCount, outputResults);
	}

	private void runWithPool (final KryoPool.Builder builder, final int runCount, final int iterCount, boolean outputResults) throws Exception {
		final KryoPool pool = builder.build();
		run("With pool " + builder.toString(), new Runnable() {
			@Override
			public void run () {
				Kryo kryo = pool.borrow();
				pool.release(kryo);
			}
		}, runCount, iterCount, outputResults);
	}

	private void systemCleanupAfterRun () throws InterruptedException {
		System.gc();
		Thread.sleep(SLEEP_BETWEEN_RUNS);
		System.gc();
	}

	private static class SampleObject {
		private int intVal;
		private float floatVal;
		private Short shortVal;
		private long[] longArr;
		private double[] dblArr;
		private String str;

		public SampleObject () {
		}

		SampleObject (int intVal, float floatVal, Short shortVal, long[] longArr, double[] dblArr, String str) {
			this.intVal = intVal;
			this.floatVal = floatVal;
			this.shortVal = shortVal;
			this.longArr = longArr;
			this.dblArr = dblArr;
			this.str = str;
		}

		/** {@inheritDoc} */
		@Override
		public boolean equals (Object other) {
			if (this == other) return true;

			if (other == null || getClass() != other.getClass()) return false;

			SampleObject obj = (SampleObject)other;

			return intVal == obj.intVal && floatVal == obj.floatVal && shortVal.equals(obj.shortVal)
				&& Arrays.equals(dblArr, obj.dblArr) && Arrays.equals(longArr, obj.longArr) && (str == null ? obj.str == null : str.equals(obj.str));
		}
	}

	
}
