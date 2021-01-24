package com.github.nullptr7
package domain

import scala.util.Try

case class CovidData(country: String, implicit val newCases: Option[Int], implicit val newDeaths: Option[Int])

object CovidData {
  implicit def convertToInt(x: String): Option[Int] = {
    Some(Try(x.toDouble.toInt).getOrElse(0))
  }
}

/*
-- Downloaded from sample csv file


iso_code --- 0
continent -- 1
location --- 2
date -- 3
total_cases -- 4
new_cases --- 5
new_cases_smoothed --6
total_deaths -- 7
new_deaths -- 8
new_deaths_smoothed
total_cases_per_million
new_cases_per_million
new_cases_smoothed_per_million
total_deaths_per_million
new_deaths_per_million
new_deaths_smoothed_per_million
reproduction_rate
icu_patients
icu_patients_per_million
hosp_patients
hosp_patients_per_million
weekly_icu_admissions
weekly_icu_admissions_per_million
weekly_hosp_admissions
weekly_hosp_admissions_per_million
total_tests
new_tests
total_tests_per_thousand
new_tests_per_thousand
new_tests_smoothed
new_tests_smoothed_per_thousand
positive_rate
tests_per_case
tests_units
total_vaccinations
new_vaccinations
new_vaccinations_smoothed
total_vaccinations_per_hundred
new_vaccinations_smoothed_per_million
stringency_index
population
population_density
median_age
aged_65_older
aged_70_older
gdp_per_capita
extreme_poverty
cardiovasc_death_rate
diabetes_prevalence
female_smokers
male_smokers
handwashing_facilities
hospital_beds_per_thousand
life_expectancy
human_development_index

* */