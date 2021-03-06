package com.littlestore.michael.maderich.entity;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@IdClass(OrderDetailId.class)
@Table(name="orderDetail")
public class OrderDetail implements Serializable {

	private static final long serialVersionUID = 123453367890L;

	@Id
	@ManyToOne(targetEntity=Order.class, fetch=FetchType.LAZY)
	@JoinColumn(name="orderNum", nullable=false)
	private Order order;
	
	@Id
	@ManyToOne(targetEntity=Product.class, fetch=FetchType.LAZY)
	@JoinColumn(name="upc", nullable=false)
	private Product product;
	
	@Basic
	@Column(name="qty", nullable=false)
	private int qty;
	
	@Basic
	@Column(name="price", nullable=false)
	private float price;
	
	@Basic
	@Column(name="lineNumber", nullable=false)
	private int lineNumber;

	public OrderDetail() {
	}

	public OrderDetail(Order order, Product product, int qty, float price, int lineNumber) {
		this.order = order;
		this.product = product;
		this.qty = qty;
		this.price = price;
		this.lineNumber = lineNumber;
	}

	
	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}

	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQty() {
		return qty;
	}
	public void setQty(int qty) {
		this.qty = qty;
	}

	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		this.price = price;
	}

	public int getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	

}
