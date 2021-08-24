/**
 * Service implementation for Customer
 */
package com.otsi.kalamandhir.serviceimpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.otsi.kalamandhir.mapper.BarCodeMapper;
import com.otsi.kalamandhir.mapper.CustomerMapper;
import com.otsi.kalamandhir.mapper.TagCustomerMapper;
import com.otsi.kalamandhir.model.Barcode;
import com.otsi.kalamandhir.model.Customer;
import com.otsi.kalamandhir.model.TagCustomer;
import com.otsi.kalamandhir.repo.BarcodeRepo;
import com.otsi.kalamandhir.repo.CustomerRepo;
import com.otsi.kalamandhir.repo.TagCustomerRepo;
import com.otsi.kalamandhir.service.CustomerService;
import com.otsi.kalamandhir.vo.CustomerVo;
import com.otsi.kalamandhir.vo.SearchFilterVo;
import com.otsi.kalamandhir.vo.TagCustomerVo;

/**
 * @author vasavi
 */
@Component
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	private CustomerRepo customerRepo;

	@Autowired
	private CustomerMapper customerMapper;

	@Autowired
	private TagCustomerRepo tagCustomerRepo;

	@Autowired
	private BarcodeRepo barCodeRepo;

	@Autowired
	private BarCodeMapper barCodeMapper;

	@Autowired
	private TagCustomerMapper tagCustomerMapper;

	@Override
	public CustomerVo generateReturnSlip(CustomerVo vo) {
		List<TagCustomerVo> tagCustomerVos = new ArrayList<>();

		if (!customerRepo.existsByMobileNumber(vo.getMobileNumber())) {
			throw new RuntimeException("mobile number doesnt exists with record :" + vo.getMobileNumber());
		}
		// here,will loop
		vo.getTagCustomerVos().stream().forEach(vos -> {
			// here ,we can find barcode through id
			Optional<Barcode> barcode = barCodeRepo.findById(vos.getBarcodeVo().getId());
			// if barcode is present,will map,set that barcode and add to tag_customer
			if (barcode.isPresent()) {
				if (barcode.get().getBarcodeId() == vos.getBarcodeVo().getBarcodeId()) {
					Barcode barCode = barCodeMapper.mapVoToEntity(vos.getBarcodeVo());
					barCode = barCodeRepo.save(barCode);
					vos.setBarcodeVo(barCodeMapper.EntityToVo(barCode));
					tagCustomerVos.add(vos);
				}
			}
		});
		// if tagCustomer size is greater than zero,will set tag_customer and return
		// customer
		if (tagCustomerVos.size() > 0) {
			vo.setTagCustomerVos(tagCustomerVos);
			return vo;
		}
		throw new RuntimeException("Generate Slip Message wasn't saved");

	}

	@Override
	public CustomerVo searchbyName(SearchFilterVo vo) {

		String mobileNo = "";

		Customer customer = null;
		Set<TagCustomer> tagCustList = new HashSet<>();
		List<TagCustomer> tagCusts = new ArrayList<>();
		CustomerVo custVo = new CustomerVo();
		if (vo.getBarcodeVo() == 0) {
			vo.setBarcodeVo(0);
		}
		if (null == vo.getBillNo()) {
			vo.setBillNo("");
		}
		if (null != vo.getMobileNo()) {
			mobileNo = vo.getMobileNo();
		}
		// here,will findByMobileNumber Or BillNoOr BarcodeBarcodeId and will assign
		// that to variable "findByAny"
		List<TagCustomer> findByAny = tagCustomerRepo.findByMobileNumberOrBillNoOrBarcodeBarcodeId(mobileNo,
				vo.getBillNo(), vo.getBarcodeVo());
		// if "findByAny" size is greater than Zero,it will go inside
		if (findByAny.size() > 0) {

			if (null != vo.getMobileNo()) {
				mobileNo = vo.getMobileNo();
			} else {
				mobileNo = findByAny.get(0).getMobileNumber();
			}

			Optional<Customer> custOpt = customerRepo.findByMobileNumber(mobileNo);
			// if it is present,will get that customer and add all to tagCustList
			if (custOpt.isPresent()) {
				customer = custOpt.get();
			}
			tagCustList.addAll(findByAny);
		}

		if (tagCustList.size() > 0) {
			custVo = customerMapper.EntityToVo(customer);
			// here, will set tag_customer to customer and return customer, if need will
			// sort based on BarCode Id in Asc Order
			// tagCusts =
			// tagCustList.stream().sorted((c1,c2)->c1.getBarcode().getBarcodeId().compareTo(c2.getBarcode().getBarcodeId())).collect(Collectors.toList());

			tagCusts = tagCustList.stream().collect(Collectors.toList());

			custVo.setTagCustomerVos(tagCustomerMapper.EntityToVo(tagCusts));
		}
		return custVo;
	}

	@Override
	public CustomerVo saveCustomer(CustomerVo vo) {
		List<TagCustomer> custList = new ArrayList<>();
		// if mobile no is already exists in db, will throw runtime exception
		if (customerRepo.existsByMobileNumber(vo.getMobileNumber())) {
			throw new RuntimeException("mobile number doesnt exists with record :" + vo.getMobileNumber());
		}
		Customer customer = customerMapper.mapVoToEntity(vo);
		customer = customerRepo.save(customer);
		// here,will loop tag_customer
		vo.getTagCustomerVos().stream().forEach(vos -> {
			Barcode barcode = barCodeMapper.mapVoToEntity(vos.getBarcodeVo());
			barcode = barCodeRepo.save(barcode);
			TagCustomer tagCustomer = tagCustomerMapper.mapVoToEntity(vos);
			tagCustomer.setBarcode(barcode);
			tagCustomer = tagCustomerRepo.save(tagCustomer);
			custList.add(tagCustomer);

		});
		// customer list is greater than zero,will map and set customer to tag_customer
		if (custList.size() > 0) {
			vo = customerMapper.EntityToVo(customer);
			vo.setTagCustomerVos(tagCustomerMapper.EntityToVo(custList));
			return vo;
		}

		throw new RuntimeException(" Customer Message wasn't saved");
	}

}
