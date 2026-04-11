package com.app.controller;

import com.app.model.Lead;
import com.app.service.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/partner")
public class PartnerController {
    
    @Autowired
    private LeadService leadService;
    
    @PostMapping("/submit")
    public String submitPartnerForm(@RequestParam String name,
                                    @RequestParam String mobile,
                                    @RequestParam String address,
                                    @RequestParam String district,
                                    @RequestParam String interestedIn,
                                    RedirectAttributes redirectAttributes) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setMobile(mobile);
        lead.setAddress(address);
        lead.setDistrict(district);
        lead.setInterestedIn(interestedIn);
        lead.setStatus("NEW");
        
        leadService.saveLead(lead);
        
        redirectAttributes.addFlashAttribute("partnerSuccess", "Thank you for your partnership interest! We will contact you shortly.");
        return "redirect:/#contact";
    }
}
