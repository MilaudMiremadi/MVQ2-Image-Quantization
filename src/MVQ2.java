/**
 * MVQ2 256 color image quantization algorithm.
 * 
 * @author Milaud Miremadi
 *
 */
public class MVQ2 {

	private static int[] palette = new int[256];
	private static Group[] map = new Group[4096];
	private static int min_size = 0;
	private static int colors = 0;

	static {
		for (int i = 0; i < map.length; i++) {
			map[i] = new Group(0);
		}
	}

	/**
	 * Converts an image to a 256 color image.
	 * The image is modified in-place.
	 * 
	 * @param image the image
	 * @return the palette
	 */
	static int[] to256(int[] image) {
		for (int i = 0; i < map.length; i++) {
			map[i].clear();
		}
		min_size = 0;
		group(image);
		do {
			prune(min_size);
		} while (colors > 256);
		create_palette();
		index(image);
		int count = 0;
		for (int i = 0; i < map.length; i++) {
			if (map[i].n > 0) {
				count++;
			}
		}
		System.out.println("Reduced to " + count + " colors.");
		return palette;
	}

	/**
	 * Create the palette by finding the averages of the groups.
	 */
	private static void create_palette() {
		int idx = 0;
		for (int i = 0; i < map.length; i++) {
			if (map[i].n != 0) {
				palette[idx++] = map[i].avg();
			}
		}
	}

	private static void index(int[] image) {
		for (int i = 0; i < image.length; i++) {
			image[i] = palette[nearest_entry(palette, image[i])];
			//Uncomment this and comment out the line above to replace the image values with palette indices instead
			//image[i] = nearest_entry(palette, image[i]);
		}
	}

	/**
	 * Group the image colors by downsampling the 24-bit colors into 12 bits.
	 * @param image the image
	 */
	private static void group(int[] image) {
		for (int i = 0; i < image.length; i++) {
			map[downsample444(image[i])].add(image[i]);
		}
	}

	/**
	 * Prune the smallest groups and merge them into their neighbors.
	 * @param min the smallest group
	 */
	private static void prune(int min) {
		boolean stop_pruning = false;
		min_size = 0x7fffffff;
		int c = colors;
		colors = 0;
		for (int i = 0; i < map.length; i++) {
			int size = map[i].n;
			if (size != 0) {
				if (size < min_size) {
					min_size = size;
				}
				if (size <= min) {
					if (!stop_pruning) {
						if (i > 0) {
							map[i - 1].add(map[i]);
						}
						c--;
						map[i].clear();
					}
					if (c < 256) {
						stop_pruning = true;
					}
				}
				colors++;
			}
		}
	}

	/**
	 * Downsample a 24-bit color into 12 bits.
	 * @param c the 24-bit color
	 * @return the 12-bit color
	 */
	private static int downsample444(int c) {
		return (((c >> 20 & 0xf) << 8) | ((c >> 12 & 0xf) << 4) | ((c >> 4 & 0xf))) & 0xfff;
	}

	/**
	 * Find the palette entry that is closest to the given color.
	 * @param pal the palette
	 * @param c the color
	 * @return the palette index
	 */
	private static int nearest_entry(int[] pal, int c) {
		int index = 0;
		float min = dist(c, pal[0]);
		for (int i = 1; i < pal.length; i++) {
			float d = dist(c, pal[i]);
			if (d < min) {
				min = d;
				index = i;
			}
		}
		return index;
	}

	/**
	 * Slightly modified color distance formula taken from:
	 * https://www.compuphase.com/cmetric.htm under the
	 * "A low-cost approximation" section.
	 * @param a color 1
	 * @param b color 2
	 * @return the distance between the two
	 */
	private static float dist(int a, int b) {
		float rbar = ((a >> 16 & 0xff) + (b >> 16 & 0xff)) / 2.0f;
		int dr = (a >> 16 & 0xff) - (b >> 16 & 0xff);
		int dg = (a >> 8 & 0xff) - (b >> 8 & 0xff);
		int db = (a & 0xff) - (b & 0xff);
		return (2.0f + rbar / 256.0f) * (dr * dr) + ((dg * dg) << 2) + (2.0f + (255.0f - rbar) / 256.0f) * (db * db);
	}

	/**
	 * A group of colors
	 * 
	 * @author Milaud Miremadi
	 *
	 */
	private static class Group {
		
		// Totals for each channel and 'n' which is the number of colors in the group.
		private int r, g, b, n;

		/**
		 * Create a group with the given color.
		 * @param c the color
		 */
		Group(int c) {
			add(c);
		}

		/**
		 * Add a color to the group.
		 * @param c the color
		 */
		void add(int c) {
			r += c >> 16 & 0xff;
			g += c >> 8 & 0xff;
			b += c & 0xff;
			n++;
		}

		/**
		 * Merge another group with this group.
		 * @param o the other group
		 */
		void add(Group o) {
			r += o.r;
			g += o.g;
			b += o.b;
			n += o.n;
		}

		/**
		 * Find the color average of this group.
		 * @return the average
		 */
		int avg() {
			if (n == 0) {
				return 0;
			}
			int hn = n >> 1;
			return ((((r + hn) / n) & 0xff) << 16 | (((g + hn) / n) & 0xff) << 8 | (((b + hn) / n) & 0xff)) & 0xffffff;
		}

		/**
		 * Reset this group to black
		 */
		void clear() {
			r = g = b = n = 0;
		}
	}
}