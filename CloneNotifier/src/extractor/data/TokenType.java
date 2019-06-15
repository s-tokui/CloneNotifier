package extractor.data;

/**
 * トークンの種類.
 * 便宜上の分類のみのため, 正式な種別ではない.
 * @author m-sano
 *
 */
public enum TokenType {

	/** 識別子名. */
	IDENTIFIER,

	/** 数値. */
	NUMBER,

	/** 予約語. */
	RESERVED,

	/** その他記号.  */
	SYMBOL,

	UNKNOWN,
}
