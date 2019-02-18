/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sagebits.tmp.isaac.rest;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 
 * {@link PaginationUtil}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class PaginationUtil {
	public static <E> Optional<List<E>> paginate(List<E> list, int pageSize, int pageNum) {
		if (pageNum < 1) {
			throw new IllegalArgumentException("pageNum (" + pageNum + ") must be >= 1");
		}
		if (pageSize < 1) {
			throw new IllegalArgumentException("pageSize (" + pageSize + ") must be >= 1");
		}
		// TODO optimize this, knowing pageNum
		List<E> rows = IntStream.iterate(0, i -> i + pageSize)
				.limit((list.size() + pageSize - 1) / pageSize)
				.boxed()
				.collect(toMap(i -> i / pageSize + 1,
						i -> list.subList(i, min(i + pageSize, list.size())))).get(pageNum);
		
		return rows != null ? Optional.of(rows) : Optional.empty();
	}

	public static <E> Map<Integer, List<E>> paginate(List<E> list, int pageSize) {
		if (pageSize < 1) {
			throw new IllegalArgumentException("pageSize (" + pageSize + ") must be >= 1");
		}
		return IntStream.iterate(0, i -> i + pageSize)
				.limit((list.size() + pageSize - 1) / pageSize)
				.boxed()
				.collect(toMap(i -> i / pageSize + 1,
						i -> list.subList(i, min(i + pageSize, list.size()))));
	}

	/**
	 * 
	 * {@link Assert} private class to mock TestNG methods so main() can be run
	 * on same code as used in unit test
	 *
	 */
	private static class Assert {
		private Assert() {}
		
		static void assertTrue(Boolean expr) {
			if (! expr) {
				throw new RuntimeException("NOT TRUE");
			}
		}
		static void assertFalse(Boolean expr) {
			if (expr) {
				throw new RuntimeException("NOT FALSE");
			}
		}

		static void assertEquals(Object actual, Object expected) {
			if (actual == expected) {
				return;
			}
			
			if (actual.equals(expected)) {
				return;
			}

			throw new RuntimeException(actual + " != " + expected);
		}

		static void assertNotNull(Object obj) {
			if (obj == null) {
				throw new RuntimeException("NULL");
			}
		}
	}
	/**
	 * @param args array of elements corresponding, respectively, to fullResultSize, maxPageSize and pageNum
	 */
	public static void main(String...args) {
		System.out.println("Args: " + Arrays.toString(args));

		// Create a fullResult of size 95
		final int fullResultSize = 95;
		List<List<String>> fullResult = new LinkedList<>();
		for (int i = 1; i <= fullResultSize; ++i) {
			fullResult.add(Arrays.asList(new String[] { i + "", i + "", i + "", i + "", i + "" }));
		}
		
		Optional<List<List<String>>> page = Optional.empty();
		List<List<String>> expectedResult = new LinkedList<>();

		// Check request for a page in the middle of the fullResult
		int maxPageSize = 10;
		int pageNum = 2;
		page = PaginationUtil.paginate(fullResult, maxPageSize, pageNum);
		Assert.assertTrue(page.isPresent());
		expectedResult = new LinkedList<>();
		for (int row = 10; row < 20; ++row) {
			expectedResult.add(fullResult.get(row));
		}
		Assert.assertEquals(page.get(), expectedResult);

		// Check request for a page outside of the fullResult
		maxPageSize = 1000;
		pageNum = 2;
		page = PaginationUtil.paginate(fullResult, maxPageSize, pageNum);
		Assert.assertFalse(page.isPresent());

		// Throw IllegalArgumentException on bad maxPageSize
		IllegalArgumentException caughtException = null;
		try {
			maxPageSize = -1;
			pageNum = 2;
			page = PaginationUtil.paginate(fullResult, maxPageSize, pageNum);
		} catch (IllegalArgumentException e) {
			caughtException = e;
		}
		Assert.assertNotNull(caughtException);

		// Throw IllegalArgumentException on bad pageNum
		caughtException = null;
		try {
			maxPageSize = 10;
			pageNum = 0;
			page = PaginationUtil.paginate(fullResult, maxPageSize, pageNum);
		} catch (IllegalArgumentException e) {
			caughtException = e;
		}
		Assert.assertNotNull(caughtException);

		// Check for page straddling the end of fullResult
		maxPageSize = 10;
		pageNum = 10;
		page = PaginationUtil.paginate(fullResult, maxPageSize, pageNum);
		Assert.assertTrue(page.isPresent());
		expectedResult = new LinkedList<>();
		for (int row = 90; row < fullResultSize; ++row) {
			expectedResult.add(fullResult.get(row));
		}
		Assert.assertEquals(page.get(), expectedResult);
	}
}