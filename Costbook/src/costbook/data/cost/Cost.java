package costbook.data.cost;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity class
 */
public class Cost implements Serializable {

	private static final long	serialVersionUID	= -1403190614544224757L;
	private int					id;
	private Date				date;
	private String				category;
	private String				remark;
	private String				currency;
	private double				amount;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}
}
