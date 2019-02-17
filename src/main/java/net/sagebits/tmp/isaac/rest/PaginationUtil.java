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
		List<E> rows = IntStream.iterate(0, i -> i + pageSize)
				.limit((list.size() + pageSize - 1) / pageSize)
				.boxed()
				.collect(toMap(i -> i / pageSize + 1,
						i -> list.subList(i, min(i + pageSize, list.size())))).get(pageNum);
		
		return rows != null ? Optional.of(rows) : Optional.empty();
	}

	public static <E> Map<Integer, List<E>> paginate(List<E> list, int pageSize) {
		return IntStream.iterate(0, i -> i + pageSize)
				.limit((list.size() + pageSize - 1) / pageSize)
				.boxed()
				.collect(toMap(i -> i / pageSize + 1,
						i -> list.subList(i, min(i + pageSize, list.size()))));
	}

	/**
	 * @param args array of elements corresponding, respectively, to fullResultSize, maxPageSize and pageNum
	 */
	public static void main(String...args) {
		System.out.println("Args: " + Arrays.toString(args));

		final int fullResultSize = Integer.parseInt(args[0]);
		List<List<String>> fullResult = new LinkedList<>();
		for (int i = 1; i <= fullResultSize; ++i) {
			fullResult.add(Arrays.asList(new String[] { i + "", i + "", i + "", i + "", i + "" }));
		}
		
		final int maxPageSize = Integer.parseInt(args[1]);
		final int pageNum = Integer.parseInt(args[2]);
		System.out.println("Displaying page " + pageNum + " (maxPageSize=" + maxPageSize + "): ");
		Optional<List<List<String>>> page = paginate(fullResult, maxPageSize, pageNum);
		if (page.isPresent()) {
			for (int row = 0; row < page.get().size(); ++ row) {
				System.out.printf("Row %03d: %s\n", row + 1, page.get().get(row));
			}
		}
	}
}