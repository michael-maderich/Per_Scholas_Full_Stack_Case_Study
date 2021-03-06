package com.littlestore.michael.maderich.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.littlestore.michael.maderich.entity.Cart;
import com.littlestore.michael.maderich.entity.CartDetail;
import com.littlestore.michael.maderich.entity.Customer;
import com.littlestore.michael.maderich.entity.Order;
import com.littlestore.michael.maderich.entity.OrderDetail;
import com.littlestore.michael.maderich.entity.Product;
import com.littlestore.michael.maderich.service.CartDetailService;
import com.littlestore.michael.maderich.service.CartService;
import com.littlestore.michael.maderich.service.CustomerService;
import com.littlestore.michael.maderich.service.OrderDetailService;
import com.littlestore.michael.maderich.service.OrderService;
import com.littlestore.michael.maderich.service.ProductService;
import com.littlestore.michael.maderich.service.SecurityService;
import com.littlestore.michael.maderich.validator.CustomerFormValidator;

/**
 * @author Michael Maderich
 *
 */
@Controller
public class MainController {
	
	@Autowired private CustomerService customerService;
	@Autowired private ProductService productService;
	@Autowired private CartService cartService;
	@Autowired private CartDetailService cartDetailService;
	@Autowired private OrderService orderService;
	@Autowired private OrderDetailService orderDetailService;
	@Autowired private SecurityService securityService;
	@Autowired private CustomerFormValidator customerFormValidator;
	
	private List<String> listStates = Stream.of(Customer.States.values()).map(Enum::name).collect(Collectors.toList());

	private List<String> listPayTypes = Stream.of(Customer.PaymentMethods.values()).map(Enum::name).collect(Collectors.toList());
	
	private	List<String> getNavMenuItems() {return productService.listCategoryMain();}

	private List<String> getNavSubMenuItems(String categoryName) {return productService.listCategorySpecificUnderMain(categoryName);}
	
	private Customer getLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof AnonymousAuthenticationToken)) {
			String currentUserName = authentication.getName();
			return customerService.findByEmail(currentUserName);
		}
		else return null;
	}
	
	@GetMapping("/403")
	public String accessDenied() {
		  return "/403";
	}
	

	@GetMapping("/{nonense}")
	public String badUrl(Model model) {
		model.addAttribute("navMenuItems", getNavMenuItems());
		return "/index";
	}
	
	// Mapping to root/home/index page
	@GetMapping({"/", "home", "/index"})
	public String home(Model model) {
		model.addAttribute("navMenuItems", getNavMenuItems());
		return "/index";
	}

	@GetMapping("/login")
	public String login(Model model, String error, String logout) {
		if (error != null) model.addAttribute("error", "Your username and/or password is invalid.");
		if (logout != null) model.addAttribute("message", "You have been logged out successfully.");
		model.addAttribute("navMenuItems", getNavMenuItems());
		if (getLoggedInUser() != null) return "redirect:/account";		// If user is logged in, redirect to account page
		else return "/login";											// Otherwise, submit POST request to login page (handled by Spring Security)
	}

	@GetMapping("/signup")
	public String registration(Model model) {
		model.addAttribute("navMenuItems", getNavMenuItems());

		if (getLoggedInUser() != null) return "redirect:/account";		// If user is already signed in, redirect to account page.
		else {															// Otherwise, submit post request to signup page with Customer info
			model.addAttribute("customerForm", new Customer());
			model.addAttribute("listStates", listStates);
			return "/signup";
		}
	}
	
	// When registration form is submitted, the signup page sends a POST request to itself.
	// The user info submitted is validated and return back to signup page if there are errors.
	// If the info submitted is valid, persist the customer data to the databases
	@PostMapping("/signup")
	public String registration(Model model, @ModelAttribute("customerForm") Customer customerForm, BindingResult bindingResult) {
		model.addAttribute("navMenuItems", getNavMenuItems());

		customerFormValidator.validate(customerForm, bindingResult);
		if (bindingResult.hasErrors()) {
			model.addAttribute("listStates", listStates);	// States enum value list needs to be sent to signup page every time. I'm sure there's a better way to do this
			return "/signup";
		}
		else {
			customerForm.setIsEnabled(true);
			customerService.create(customerForm);
			securityService.autoLogin(customerForm.getEmail(), customerForm.getPasswordConfirm());
			model.addAttribute("listStates", listStates);
			return "/account";
		}
	}
	
	// Page for user to view/edit Profile, view Order History, and other functions TBD
	@GetMapping("/account")
	public String accountPage(Model model) {
//		model.addAttribute("navMenuItems", getNavMenuItems());

		Customer customer = getLoggedInUser();
		if (customer == null) {
			model.addAttribute("navMenuItems", getNavMenuItems());
			return "redirect:/login";
		}
		else {
			model.addAttribute("customerForm", customer);
			model.addAttribute("listStates", listStates);
			return "/account";
		}
	}
	
	// For editing a customer? I forget. Yes, I think so.
	@PostMapping("/account")
	public String accountChange(Customer customer) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof AnonymousAuthenticationToken)) {
//			String currentUserName = authentication.getName();
			customerService.update(customer);
//			model.addAttribute("listStates", listStates);
			return "/account";
		}
		return "redirect:/login";
	}

	// View account's order history - user must be logged in
	@GetMapping("/account/orders")
	public String orderHistory(Model model) {
		model.addAttribute("navMenuItems", getNavMenuItems());
		Customer customer = getLoggedInUser();
		if (customer == null) {				// Can't view orders if not logged in, for now. Direct user to log in/sign up
			model.addAttribute("error", "You must be logged in to add items to view your orders.");
			return "redirect:/login";
		}
		else {
			model.addAttribute("customer", customer);
			List<Order> orderList = orderService.findByCustomer(customer);
			model.addAttribute("orderList", orderList);
			return "/order";
		}
		
	}

	// Only need String likeName for Search, empty default returns all Products
/*	@GetMapping({ "/productList" })
		public String listProductHandler(Model model,
					@RequestParam(value = "name", defaultValue = "") String likeName,
					@RequestParam(value = "page", defaultValue = "1") int page) {
			final int maxResult = 5;
			final int maxNavigationPage = 10;
		 
			PaginationResult<Product> result = productService.queryProducts(page,
		maxResult, maxNavigationPage, likeName);
		 
		model.addAttribute("paginationProducts", result);
		return "productList";
	}*/
	
	@GetMapping("/category/")
	public String categoryRootRedirect() {
		return "redirect:/category/Laundry";
	}
	
	@GetMapping("/category/{categoryName}")
	public String listItemsInCategory(Model model, @PathVariable(name="categoryName") String categoryName,
										@RequestParam(value = "addedUpc", defaultValue="") String addedUpc,
										@RequestParam(value = "addedItemQty", defaultValue="0") String addedItemQty) {
		List<Product> itemList = productService.findByCategoryMainSorted(categoryName);
		boolean goodLink = false;
		for (Product p : itemList) if ( p.getCategoryMain().equals(categoryName) ) goodLink = true;
		if (!goodLink) return "redirect:/";

		// May need this if we want the catalog to show the quantity already in cart
//		int[] upcList = new String[itemList.size()];	// Array to hold items 
//		int[] qtyList = new int[itemList.size()];	// Array to hold items to add to cart, initialized to all 0
		Customer customer = getLoggedInUser();
		if (customer != null) {										// If a User is logged in, get their cart, (or null if it doesn't exist)
			Cart customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart != null)
			{		// If they have a cart, fill cartItems with their cart item quantities
				List<CartDetail> cartItems = customerCart.getCartItems();
				model.addAttribute("cartItems", cartItems);
			}
		}
		model.addAttribute("navMenuItems", getNavMenuItems());
		model.addAttribute("navSubMenuItems", getNavSubMenuItems(categoryName));
		model.addAttribute("addedUpc", addedUpc);
		model.addAttribute("addedItemQty", addedItemQty);
		model.addAttribute("itemList", itemList);
//		model.addAttribute("qtyList", qtyList);
		return "category";
	}

	@GetMapping("/category/{categoryName}/{subCategoryName}")
	public String listItemsInSubCategory(@PathVariable(name="categoryName") String categoryName,
										@PathVariable(name="subCategoryName") String subCategoryName, Model model,
										@RequestParam(value = "addedUpc", defaultValue="") String addedUpc,
										@RequestParam(value = "addedItemQty", defaultValue="0") String addedItemQty) {
		List<Product> itemList = productService.findByCategorySpecificSorted(subCategoryName);
		boolean goodLink = false;
		for (Product p : itemList) if ( p.getCategoryMain().equals(categoryName) 
									&& p.getCategorySpecific().equals(subCategoryName) ) goodLink = true;
		if (!goodLink) return "redirect:/";
		
		model.addAttribute("navMenuItems", getNavMenuItems());
		model.addAttribute("navSubMenuItems", getNavSubMenuItems(categoryName));
		model.addAttribute("addedUpc", addedUpc);
		model.addAttribute("addedItemQty", addedItemQty);
		model.addAttribute("itemList", itemList);
		return "category";
	}

	@GetMapping("/addToCart")
	public String addItemsToCart(HttpServletRequest request, Model model,
								@RequestParam(value = "upc", defaultValue="") String upc,
								@RequestParam(value = "itemQty", defaultValue="0") String itemQty) {

		String referer = request.getHeader("Referer");						// http://localhost:8080/xxxxxx - we just want the "xxxxxx"
		if (referer==null) return "redirect:/cart";					// If page request didn't come from the cart, reject it and return to cart
		else {
			referer = referer.substring( referer.indexOf('/', referer.indexOf("//")+2) );		// everything after root '/', inlcuding the /
			referer = referer.substring(0, (referer.indexOf('?') != -1) ? referer.indexOf('?') : referer.length());	// remove the query string if exists
			if (!referer.startsWith("/category")) return "redirect:/"+referer;
		}

		Customer customer = getLoggedInUser();
		Cart customerCart;
		Product purchasedProduct;
		int purchasedQty = Integer.parseInt(itemQty);		// Can't throw exception because referer string format already checked
		int addedItemQty = purchasedQty;
		try {												// Irrelevant since referer string checked, but maybe missed something
			purchasedProduct = productService.get(upc);
		}
		catch (NoSuchElementException e) {
			return "redirect:" + referer;
		}

		if (customer == null) {				// Can't add to cart if not logged in, for now. Direct user to log in/sign up
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "You must be logged in to add items to your cart.");
			return "/login";
		}
		else {													// If a User is logged in, get their cart, (or null if it doesn't exist)
			customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) {							// If they don't have a cart started, start a new one
				customerCart = new Cart();
				customerCart.setCustomer(customer);
				customerCart.setCartCreationDateTime(LocalDateTime.now());
				customerCart.setCartItems(new ArrayList<CartDetail>());
				cartService.save(customerCart);					// New cart needs to be saved before items can be added because of FK relationship
			}
			List<CartDetail> cartItems = new ArrayList<>(customerCart.getCartItems());
			int lineNum = 1;
			for (CartDetail item : cartItems) {
				if (item.getProduct().getUpc() == upc) {		// One or more of this item is already in the cart, so just increase qty
					purchasedQty += item.getQty();				// Add qty already in cart to amount added to cart
					if (purchasedQty > item.getProduct().getStockQty())	{	// If more than available stock is requested..
						purchasedQty = item.getProduct().getStockQty();		// Lower purchased qty to available stock
						addedItemQty = item.getProduct().getStockQty() - item.getQty();	// How many were actually added
					}
//					item.setQty(item.getQty()+purchasedQty);	// Don't set now..
					cartItems.remove(item);						// delete and recreate instead for smoother code
					lineNum = item.getLineNumber() - 1;						// Get the item's line number -1 because will ++ after loop
					break;	// out of foreach loop
				}
				else if (item.getLineNumber() > lineNum) lineNum = item.getLineNumber();	// Get new line number based on max existing
			}
			lineNum++;

			CartDetail newLineItem = new CartDetail(customerCart, purchasedProduct, purchasedQty, purchasedProduct.getCurrentPrice(), lineNum);
			cartItems.add(newLineItem);
			Collections.sort(cartItems);			// CartDetail entity contains compareTo() method. List sorted for better cart/checkout display
			customerCart.setCartItems(cartItems);
			cartDetailService.save(newLineItem);	// Will overwrite any previous cartDetail with same composite key (cartId/upc)
			cartService.save(customerCart);
/**/		System.out.println(customerCart);

			model.addAttribute("customerCart", customerCart);
			return "redirect:" + referer+"?addedUpc="+upc+"&addedItemQty="+addedItemQty;
		}
	}

	@GetMapping("/cart")
	public String cart(Model model) {
		Cart customerCart;
		Customer customer = getLoggedInUser();
		if (customer == null) {				// Can't view cart if not logged in, for now. Direct user to log in/sign up
			model.addAttribute("error", "You must be logged in to view your cart.");
			model.addAttribute("navMenuItems", getNavMenuItems());
			return "redirect:/login";
		}
		else {										// If a User is logged in, get their cart, (or null if it doesn't exist)
			customerCart = cartService.findByCustomerEmail(customer.getEmail());		// Possibly null if no cart started, but handles fine
			if (customerCart == null) {			// If they don't have a cart, redirect to cart page but couldn't get here unless url typed/bookmarked
				model.addAttribute("customer", customer);
				model.addAttribute("customerCart", null);
				return "/cart";
			}
			List<CartDetail> cartItems = customerCart.getCartItems();
			Collections.sort(cartItems);			// CartDetail entity contains compareTo() method
			customerCart.setCartItems(cartItems);
			model.addAttribute("customer", customer);
			model.addAttribute("customerCart", customerCart);
			return "/cart";
		}
	}
	
	@GetMapping("/removeFromCart")
	public String removeItemsFromCart(Model model,
								@RequestParam(value = "upc", defaultValue="") String upc) {

		Customer customer = getLoggedInUser();
		Cart customerCart;
		Product removedProduct;
		try {	// This block only necessary if bad query string, which would only happen if url entered manually
			removedProduct = productService.get(upc);
		}
		catch (NoSuchElementException e) {
			return "redirect:/cart";
		}
		if (customer == null) {		// Can't edit cart if not logged in, but also can't get here since can't access cart, either, unless url typed
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "You must be logged in to edit your cart.");
			return "/login";
		}
		else {														// If a User is logged in, get their cart, (or null if it doesn't exist)
			customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) {								// Once again, only accessible through url through bad request
				return "redirect:/cart";
			}
			List<CartDetail> cartItems = new ArrayList<>(customerCart.getCartItems());
			CartDetail removedLineItem;
			try {
				removedLineItem = cartDetailService.findLineByCartAndProduct(customerCart, removedProduct);				
			}
			catch (NoSuchElementException e) {			// An item that does not exist in the cart has been attempted to be removed, again manual URL
				return "redirect:/cart";
			}
			cartItems.remove(removedLineItem);
			Collections.sort(cartItems);			// CartDetail entity contains compareTo() method
			customerCart.setCartItems(cartItems);
			cartDetailService.deleteLineByCartAndProduct(customerCart, removedProduct);
			if (cartItems.isEmpty()) cartService.delete(customerCart);	// If customer empties cart and comes back later, we want creation time to reset
			else cartService.save(customerCart);
/**/		System.out.println(customerCart);

			model.addAttribute("customer", customer);
			model.addAttribute("customerCart", customerCart);	
			return "/cart";
		}
	}

	@GetMapping("/clearCart")
	public String removeAllItemsFromCart(HttpServletRequest request, Model model) {

		String referer = request.getHeader("Referer");				// http://localhost:8080/xxxxxx - we just want the "xxxxxx"
		if (referer==null) return "redirect:/cart";					// If page request didn't come from the cart, reject it and return to cart
		else {
			referer = referer.substring( referer.indexOf('/', referer.indexOf("//")+2) );		// everything after root '/', inlcuding the /
			referer = referer.substring(0, (referer.indexOf('?') != -1) ? referer.indexOf('?') : referer.length());	// remove the query string if exists
			if (!referer.equals("/cart")) return "redirect:/cart";
		}

		Customer customer = getLoggedInUser();
		Cart customerCart;
		if (customer == null) {		// Can't delete cart if not logged in, but also can't get here since can't access cart, either, unless url typed
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "You must be logged in to edit your cart.");
			return "/login";
		}
		else {														// If a User is logged in, get their cart, (or null if it doesn't exist)
			customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) return "redirect:/cart";		// Once again, only accessible through url through bad request, but handle it
			List<CartDetail> cartItems = new ArrayList<>(customerCart.getCartItems());
			for (CartDetail item : cartItems) cartDetailService.delete(item);
			cartService.delete(customerCart);
			model.addAttribute("customer", customer);
			return "redirect:/cart";
		}
	}

	@GetMapping("/checkout")
	public String orderFinalizationPage(Model model) {
		Customer customer = getLoggedInUser();
		if (customer == null) {				// Can't check out if not logged in, for now. Direct user to log in
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "Please log in to your account to check out.");
			return "/login";
		}
		else {														// If a User is logged in, get their cart, (or null if it doesn't exist)
			Cart customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) {			// If they don't have a cart, redirect to cart page but couldn't get here unless url typed/bookmarked
				model.addAttribute("customer", customer);
				model.addAttribute("customerCart", customerCart);
				return "redirect:/cart";
			}
			// Reorganize cart so it's ordered by category/subcategory/name/options/size
			List<CartDetail> cartItems = customerCart.getCartItems();
			Collections.sort(cartItems);			// CartDetail entity contains compareTo() method
			model.addAttribute("customerInfo", customer);
			model.addAttribute("customerCart", customerCart);
			model.addAttribute("listStates", listStates);
			model.addAttribute("listPayTypes", listPayTypes);
			return "checkout";
		}
	}

/*	@PostMapping("/checkout")
	public String orderFinalizationPage(Model model) {
		Customer customer = getLoggedInUser();
		if (customer == null) {				// Can't check out if not logged in, for now. Direct user to log in
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "Please log in to your account to check out.");
			return "/login";
		}
		else {														// If a User is logged in, get their cart, (or null if it doesn't exist)
			Cart customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) {			// If they don't have a cart, redirect to cart page but couldn't get here unless url typed/bookmarked
				model.addAttribute("customer", customer);
				model.addAttribute("customerCart", customerCart);
				return "redirect:/cart";
			}
			// Reorganize cart so it's ordered by category/subcategory/name/options/size
			List<CartDetail> cartItems = customerCart.getCartItems();
			Collections.sort(cartItems);			// CartDetail entity contains compareTo() method
			model.addAttribute("customerInfo", customer);
			model.addAttribute("customerCart", customerCart);
			model.addAttribute("listStates", listStates);
			model.addAttribute("listPayTypes", listPayTypes);
			return "checkout";
		}
	}*/

	// Move cart to order in database and delete cart
	// Remove sold items from inventory
	// Find a way to send order via email
	// Display confirmation (perhaps order details/customer info/etc again
	@PostMapping("/confirmation")
	public String completeOrder(Model model, @ModelAttribute("customerInfo") Customer customerUpdates) {
		
		Customer customer = getLoggedInUser();
		if (customer == null) {				// Can't complete order if not logged in, for now. Direct user to log in page
			model.addAttribute("navMenuItems", getNavMenuItems());
			model.addAttribute("error", "Please log in to your account to check out.");
			return "/login";
		}
		else {														// If a User is logged in, get their cart, (or null if it doesn't exist)
			Cart customerCart = cartService.findByCustomerEmail(customer.getEmail());
			if (customerCart == null) {			// If they don't have a cart, redirect to cart page but couldn't get here unless url typed/bookmarked
				model.addAttribute("customer", customer);
				model.addAttribute("customerCart", customerCart);
				return "redirect:/cart";
			}
			// Add to Customer any updates to meeting address, phone/contact, payment method and payment handle
			customer.setAddress(customerUpdates.getAddress());
			customer.setCity(customerUpdates.getCity());
			customer.setState(customerUpdates.getState());
			customer.setPreferredPayment(customerUpdates.getPreferredPayment());
			customer.setPaymentHandle(customerUpdates.getPaymentHandle());
			customerService.update(customer);

			// Convert cart to Order and delete Cart
			Order customerOrder = new Order();
			customerOrder.setCustomer(customer);
			customerOrder.setOrderDateTime(LocalDateTime.now());
			customerOrder.setReqDeliveryDateTime(null);
			customerOrder.setStatus("Confirmed");	// Will need to update this later using enum
			customerOrder.setComments(null);
			orderService.save(customerOrder);
			
			// Add each Cart Detail to Order Detail table
			List<CartDetail> cartItems = customerCart.getCartItems();
			List<OrderDetail> orderItems = new ArrayList<OrderDetail>();
			int lineNum = 1;
			for (CartDetail item : cartItems) {
				OrderDetail lineItem = new OrderDetail();
				lineItem.setOrder(customerOrder);
//				String upc = item.getProduct().getUpc();
//				Product product = productService.get(upc);
				lineItem.setProduct(item.getProduct());		//(product);
				lineItem.setQty(item.getQty());
				lineItem.setPrice(item.getPrice());
				lineItem.setLineNumber(lineNum++);
				orderItems.add(lineItem);
				orderDetailService.save(lineItem);			// Save Order line item
				cartDetailService.delete(item);				// Delete item from CartDetail table
			}
			customerOrder.setOrderItems(orderItems);
			orderService.save(customerOrder);
			cartService.delete(customerCart);				// Remove the cart from DB
			
			// Remove sold qtys from database !!!!!!!!!!!
			
			model.addAttribute("customerInfo", customer);
			model.addAttribute("customerOrder", customerOrder);
			model.addAttribute("listStates", listStates);
			model.addAttribute("listPayTypes", listPayTypes);
			return "confirmation";
		}
	}


/*	@RequestMapping({ "/buyProduct" })
	public String listProductHandler(HttpServletRequest request, Model model, //
			@RequestParam(value = "code", defaultValue = "") String code) {
 
		Product product = null;
		if (code != null && code.length() > 0) {
			product = productDAO.findProduct(code);
		}
		if (product != null) {
 
			//
			CartInfo cartInfo = Utils.getCartInSession(request);
 
			ProductInfo productInfo = new ProductInfo(product);
 
			cartInfo.addProduct(productInfo, 1);
		}
 
		return "redirect:/shoppingCart";
	}
 
	@RequestMapping({ "/shoppingCartRemoveProduct" })
	public String removeProductHandler(HttpServletRequest request, Model model, //
			@RequestParam(value = "code", defaultValue = "") String code) {
		Product product = null;
		if (code != null && code.length() > 0) {
			product = productDAO.findProduct(code);
		}
		if (product != null) {
 
			CartInfo cartInfo = Utils.getCartInSession(request);
 
			ProductInfo productInfo = new ProductInfo(product);
 
			cartInfo.removeProduct(productInfo);
 
		}
 
		return "redirect:/shoppingCart";
	}
 
	// POST: Update quantity for product in cart
	@RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
	public String shoppingCartUpdateQty(HttpServletRequest request, //
			Model model, //
			@ModelAttribute("cartForm") CartInfo cartForm) {
 
		CartInfo cartInfo = Utils.getCartInSession(request);
		cartInfo.updateQuantity(cartForm);
 
		return "redirect:/shoppingCart";
	}
 
	// GET: Show cart.
	@RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
	public String shoppingCartHandler(HttpServletRequest request, Model model) {
		CartInfo myCart = Utils.getCartInSession(request);
 
		model.addAttribute("cartForm", myCart);
		return "shoppingCart";
	}
 
	// GET: Enter customer information.
	@RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
	public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {
 
		CartInfo cartInfo = Utils.getCartInSession(request);
 
		if (cartInfo.isEmpty()) {
 
			return "redirect:/shoppingCart";
		}
		CustomerInfo customerInfo = cartInfo.getCustomerInfo();
 
		CustomerForm customerForm = new CustomerForm(customerInfo);
 
		model.addAttribute("customerForm", customerForm);
 
		return "shoppingCartCustomer";
	}
 
	// POST: Save customer information.
	@RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
	public String shoppingCartCustomerSave(HttpServletRequest request, //
			Model model, //
			@ModelAttribute("customerForm") @Validated CustomerForm customerForm, //
			BindingResult result, //
			final RedirectAttributes redirectAttributes) {
 
		if (result.hasErrors()) {
			customerForm.setValid(false);
			// Forward to reenter customer info.
			return "shoppingCartCustomer";
		}
 
		customerForm.setValid(true);
		CartInfo cartInfo = Utils.getCartInSession(request);
		CustomerInfo customerInfo = new CustomerInfo(customerForm);
		cartInfo.setCustomerInfo(customerInfo);
 
		return "redirect:/shoppingCartConfirmation";
	}
 
	// GET: Show information to confirm.
	@RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
	public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
		CartInfo cartInfo = Utils.getCartInSession(request);
 
		if (cartInfo == null || cartInfo.isEmpty()) {
 
			return "redirect:/shoppingCart";
		} else if (!cartInfo.isValidCustomer()) {
 
			return "redirect:/shoppingCartCustomer";
		}
		model.addAttribute("myCart", cartInfo);
 
		return "shoppingCartConfirmation";
	}
 
	// POST: Submit Cart (Save)
	@RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.POST)
 
	public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
		CartInfo cartInfo = Utils.getCartInSession(request);
 
		if (cartInfo.isEmpty()) {
 
			return "redirect:/shoppingCart";
		} else if (!cartInfo.isValidCustomer()) {
 
			return "redirect:/shoppingCartCustomer";
		}
		try {
			orderDAO.saveOrder(cartInfo);
		} catch (Exception e) {
 
			return "shoppingCartConfirmation";
		}
 
		// Remove Cart from Session.
		Utils.removeCartInSession(request);
 
		// Store last cart.
		Utils.storeLastOrderedCartInSession(request, cartInfo);
 
		return "redirect:/shoppingCartFinalize";
	}
 
	@RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
	public String shoppingCartFinalize(HttpServletRequest request, Model model) {
 
		CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);
 
		if (lastOrderedCart == null) {
			return "redirect:/shoppingCart";
		}
		model.addAttribute("lastOrderedCart", lastOrderedCart);
		return "shoppingCartFinalize";
	}
 
	@RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
	public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam("code") String code) throws IOException {
		Product product = null;
		if (code != null) {
			product = this.productDAO.findProduct(code);
		}
		if (product != null && product.getImage() != null) {
			response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
			response.getOutputStream().write(product.getImage());
		}
		response.getOutputStream().close();
	}*/
}