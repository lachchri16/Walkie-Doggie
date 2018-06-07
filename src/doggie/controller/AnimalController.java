package doggie.controller;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import doggie.animals.dao.ACRepository;
import doggie.animals.dao.AnimalRepository;
import doggie.animals.dao.CompatibilityRepository;
import doggie.animals.dao.ImageRepository;
import doggie.animals.dao.SpeciesRepository;
import doggie.animals.dao.VaccinationRepository;
import doggie.animals.model.AC;
import doggie.animals.model.AnimalImage;
import doggie.animals.model.AnimalModel;
import doggie.animals.model.Species;
import doggie.animals.model.Vaccination;

@Controller
public class AnimalController {

	@Autowired
	AnimalRepository animalRepository;

	@Autowired
	CompatibilityRepository compatibilityRepository;

	@Autowired
	VaccinationRepository vaccinationRepository;
	
	@Autowired
	SpeciesRepository speciesRepository;

	@Autowired
	ACRepository acRepository;

	@Autowired
	ImageRepository imageRepository;

	@RequestMapping(value = "/petbook")
	public String petbook(Model model) {

		List<AnimalModel> animals = animalRepository.findAll();

		model.addAttribute("animals", animals);

		return "petbook";
	}

	@RequestMapping(value = { "/profil" })
	public String profil(Model model, @RequestParam int id) {
		Optional<AnimalModel> animalOpt = animalRepository.findById(id);

		if (!animalOpt.isPresent())
			throw new IllegalArgumentException("No animal with id " + id);

		AnimalModel animal = animalOpt.get();

		model.addAttribute("animal", animal);
		List<Vaccination> vaccinations = vaccinationRepository.findAllByAnimals(animal);
		model.addAttribute("vaccinations", vaccinations);
		List<AC> acs = acRepository.findAllByAnimal(animal);
		model.addAttribute("acs", acs);
		List<AnimalImage> images = imageRepository.findAllByAnimal(animal);
		model.addAttribute("images", images);

		return "profil";
	}

	@RequestMapping(value = "/addAnimal", method = RequestMethod.GET)
	public String showAddAnimalForm(Model model) {
		List<Species> species = speciesRepository.findAll();
		model.addAttribute("species", species);
		
		List<Vaccination> vaccinations = vaccinationRepository.findAll();
		model.addAttribute("vaccinations", vaccinations);
		model.addAttribute("selectedVaccinations", new ArrayList<Vaccination>());
		return "editAnimal";
	}

	@RequestMapping(value = "/editAnimal", method = RequestMethod.GET)
	public String showChangeAnimalForm(Model model, @RequestParam int id) {

		Optional<AnimalModel> animalOpt = animalRepository.findById(id);

		if (!animalOpt.isPresent())
			throw new IllegalArgumentException("No animal with id " + id);

		AnimalModel animal = animalOpt.get();
		model.addAttribute("animal", animal);
		
		List<Species> species = speciesRepository.findAll();
		model.addAttribute("species", species);
		
		List<Vaccination> vaccinations = vaccinationRepository.findAll();
		model.addAttribute("vaccinations", vaccinations);
		
		List<Vaccination> selectedVaccinations = vaccinationRepository.findAllByAnimals(animal);
		List<Integer> selectedV = selectedVaccinations.stream().map(v -> v.getId()).collect(Collectors.toList());;
				
		model.addAttribute("selectedV", selectedV);
		
		return "editAnimal";
	}
	
	@RequestMapping(value = "/addAnimal", method = RequestMethod.POST)
	public String addAnimal(@Valid AnimalModel newAnimalModel, BindingResult bindingResult, Model model,
			@RequestParam("species") int species,
			@RequestParam("vaccination") List<Integer> vaccination) {
		
		if (bindingResult.hasErrors()) {
			String errorMessage = "";
			for (FieldError fieldError : bindingResult.getFieldErrors()) {
				errorMessage += fieldError.getField() + " is invalid<br>";
			}
			model.addAttribute("errorMessage", errorMessage);
			return "forward:/petbook";
		}
		
		Optional<AnimalModel> animalOpt = animalRepository.findById(newAnimalModel.getId());

		if (animalOpt.isPresent())
			model.addAttribute("errorMessage", "Animal does exist!<br>");
		else {
			newAnimalModel.setSpecies(speciesRepository.findById(species).get());
			newAnimalModel.setVaccinations(vaccinationRepository.findByIdIn(vaccination));
			animalRepository.save(newAnimalModel);
			
			model.addAttribute("message", "New Animal " + newAnimalModel.getId() + " added.");
			
		}
		
		return "forward:/petbook";
	}
	
	@RequestMapping(value = "/editAnimal", method = RequestMethod.POST)
	public String editAnimal(@Valid AnimalModel changedAnimalModel, BindingResult bindingResult, Model model,
			@RequestParam("species") int species,
			@RequestParam("vaccination") List<Integer> vaccination) {

		if (bindingResult.hasErrors()) {
			String errorMessage = "";
			for (FieldError fieldError : bindingResult.getFieldErrors()) {
				errorMessage += fieldError.getField() + " is invalid<br>";
			}
			model.addAttribute("errorMessage", errorMessage);
			return "forward:/petbook";
		}
 
		Optional<AnimalModel> animalOpt = animalRepository.findById(changedAnimalModel.getId());

		if (!animalOpt.isPresent())
			model.addAttribute("errorMessage", "Animal does not exist!<br>");
		else {

		AnimalModel animal = animalOpt.get();
 
		animal.setId(changedAnimalModel.getId());
		animal.setName(changedAnimalModel.getName());
		animal.setBreed(changedAnimalModel.getBreed());
		animal.setColor(changedAnimalModel.getColor());
		animal.setAge(changedAnimalModel.getAge());
		animal.setGender(changedAnimalModel.getGender());
		animal.setCastrated(changedAnimalModel.isCastrated());
		animal.setDescription(changedAnimalModel.getDescription());
		animal.setSpecies(speciesRepository.findById(species).get());
		animal.setVaccinations(vaccinationRepository.findByIdIn(vaccination));
		animalRepository.save(animal);

		model.addAttribute("message", "Changed animal " + changedAnimalModel.getId());
		}
		return "forward:/petbook";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String showUploadForm(Model model, @RequestParam("id") int animalId) {
		model.addAttribute("animalId", animalId);
		return "uploadFile";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String uploadDocument(Model model, @RequestParam("id") int animalId,
			@RequestParam("myFile") MultipartFile file) {

		try {

			Optional<AnimalModel> animalOpt = animalRepository.findById(animalId);
			if (!animalOpt.isPresent())
				throw new IllegalArgumentException("No animal with id " + animalId);

			AnimalModel animal = animalOpt.get();

			AnimalImage image = new AnimalImage();
			image.setContent(file.getBytes());
			image.setContentType(file.getContentType());
			image.setFilename(file.getOriginalFilename());
			image.setName(file.getName());
			image.setProfile(true);
			image.setAnimal(animal);
			animal.addImage(image);
			imageRepository.save(image);

		} catch (Exception e) {
			model.addAttribute("errorMessage", "Error:" + e.getMessage());
		}

		return "forward:/profil";
	}

	@RequestMapping("/animalImage")
	public void download(@RequestParam("id") int imageId, HttpServletResponse response) {

		Optional<AnimalImage> imgOpt = imageRepository.findById(imageId);
		if (!imgOpt.isPresent())
			throw new IllegalArgumentException("No image with id " + imageId);

		AnimalImage img = imgOpt.get();

		try {
			OutputStream out = response.getOutputStream();
			response.setContentType(img.getContentType());
			out.write(img.getContent());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
