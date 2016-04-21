package hello.controller;

import hello.model.Soldier;
import hello.repository.SoldierRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bjagwani on 4/13/16.
 */
@Controller
public class HelloController {

    @Autowired
    private SoldierRepository soldierRepository;

    @RequestMapping(value="/index", method=RequestMethod.GET)
    public String index(Model model){
        return "index";
    }

    @RequestMapping(value= "/addSoldier", method=RequestMethod.POST)
    public String addSimianSoldier(@ModelAttribute Soldier soldier, Model model){
        System.out.println("Name of soldier: " + soldier.getName());
        soldierRepository.save(soldier);

        Iterator<Soldier> iterator= soldierRepository.findAll().iterator();

        List<Soldier> soldiers= new ArrayList<Soldier>();
        while(iterator.hasNext())
            soldiers.add(iterator.next());

        model.addAttribute("soldiers", soldiers);
        model.addAttribute("soldier", soldier);
        return "result";
    }


    @RequestMapping(value="/listSoldiers", method=RequestMethod.GET)
    public String listSoldiers(Model model){

        Iterator<Soldier> iterator= soldierRepository.findAll().iterator();

        List<Soldier> soldiers= new ArrayList<Soldier>();
        while(iterator.hasNext())
            soldiers.add(iterator.next());

        model.addAttribute("soldiers", soldiers);
        return "list";
    }

    @RequestMapping(value="/add", method=RequestMethod.POST)
    @ResponseBody
    public String add(@RequestBody Soldier soldier){
        soldierRepository.save(soldier);
        return "Added";
    }


    @RequestMapping(value="/list", method=RequestMethod.GET)
    @ResponseBody
    public String list(){
       return String.valueOf(soldierRepository.count());
    }

}
